import java.util.Scanner;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;


public class SimBase {
    
    /** Returns the bits of number between idx1 and idx2 as an integer */
    public int get_bits(int number, int idx1, int idx2) {
        int low = idx1 < idx2 ? idx1 : idx2;
        int num = idx1 < idx2 ? idx2-idx1 : idx1-idx2;
        return (number >> low) & ((1<<num)-1);
    }
    /** Handles a single instruction, returning the new PC */
    public int execute(byte instruction, byte oldPC) {
        int reserved = get_bits(instruction, 7, 8);
        if ((reserved) == 1) {
            return oldPC;
        } 
        else {
            int A = get_bits(instruction, 2, 4);
            int B = get_bits(instruction, 0, 2);
            int icode = get_bits(instruction, 4, 7);
            if (icode == 0) {
                R[A] = R[B];
            } 
            else if (icode == 1) {
                R[A] += R[B];
            } 
            else if (icode == 2) {
                R[A] &= R[B];
            }
            else if (icode == 3) {
                R[A] = M[R[B] & 0xFF];
            }
            else if (icode == 4) {
                M[R[B] & 0xFF] = R[A];
            }
            else if (icode == 5) {
                if (B == 0) {
                    R[A] = (byte) ~R[A];
                } 
                else if (B == 1) {
                    R[A] = (byte) -R[A];
                } 
                else if (B == 2) {
                    if (R[A] == 0) {
                        R[A] = 1;
                    } 
                    else {
                        R[A] = 0;
                    }
                } 
                else if (B == 3) {
                    R[A] = oldPC;
                }
            }
            else if (icode == 6) {
                if (B == 0) {
                    R[A] = M[oldPC + 1];
                } 
                else if (B == 1) {
                    R[A] += M[oldPC + 1];
                } 
                else if (B == 2) {
                    R[A] &= M[oldPC + 1];
                } 
                else if (B == 3) {
                    R[A] = M[M[oldPC + 1] & 0xff];
                }
                oldPC++;
            }
            else if (icode == 7) {
                if (R[A] <= 0) {
                    return R[B];
               }
            }

            return oldPC + 1;   
        }  
    }
        // to do: add instructions here

        


    // memory and registers
    public byte[] M;
    public byte[] R;

    // control registers; do not modify these directly
    private byte _pc;
    private byte _ir;
    
    /** Constructor, initializing memory from a file */
    public SimBase(Path filepath) throws java.io.IOException {
        M = new byte[256];
        R = new byte[4];
        Scanner s =  new Scanner(Files.newInputStream(filepath));
        int i = 0;
        while(s.hasNext()) {
            M[i] = (byte)s.nextShort(16); // cast because Java has signed bytes
            i += 1;
        }
    }
    /** Constructor, initializing memory from bytes on the command line */
    public SimBase(String[] bytes) {
        M = new byte[256];
        R = new byte[4];
        for(int i=0; i<256 && i < bytes.length; i+=1) {
            M[i] = (byte)Short.parseShort(bytes[i], 16); // cast because Java has signed bytes
        }
    }
    
    /** Helper function since Java lacks Byte.toString(value, radix) or binary printf flags */
    public static String toBin(int n, int width) {
        String ans = "";
        for(int i=0; i<width; i+=1) {
            ans = (char)('0' + (n&1)) + ans;
            n >>= 1;
        }
        return ans;
    }
    
    /** Displays all processor state to command line */
    public void showState() {
        System.out.println("----------------------------------------");
        System.out.printf("last instruction = 0b%s (0x%02x)\n", toBin(_ir, 8), _ir);
        for(int i=0; i<4; i+=1) {
            System.out.printf("Register %s = 0b%s (0x%02x)\n", toBin(i, 2), toBin(R[i], 8), R[i]);
        }
        System.out.printf("next PC = 0b%s (0x%02x)\n", toBin(_pc, 8), _pc);
        System.out.println("//////////////////////// Memory \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\");
        for(int i=0; i<256; i+=16) {
            System.out.printf("0x%02x-%02x: ", i, i+15);
            for(int j=0; j<16; j+=1) {
                System.out.printf("%02x ", M[i+j]);
            }
            System.out.println();
            boolean all0 = true;
            for(int j=i+16; j<M.length; j+=1) all0 &= (M[j] == 0); 
            if (all0) break;
        }
        System.out.println("----------------------------------------");
    }
    
    
    /** Implements one clock cycle */
    public void cycle() {
        _ir = M[_pc];
        _pc = (byte)execute(_ir, _pc);
    }


    
    public static void main(String[] args) {
        if (args.length <= 0) {
            System.err.println("USAGE: java SimBase memory.txt\n    where memory.txt is a set of bytes in hex");
            System.err.println("USAGE: java SimBase byte [byte, byte, ...]\n    where the bytes are in hex and will be loaded into memory before running");
            System.exit(1);
        }
        SimBase simulator;
        try {
            Path p = Paths.get(args[0]).toRealPath();
            simulator = new SimBase(p);
        } catch (java.io.IOException ex) {
            simulator = new SimBase(args);
        }
        simulator.showState();
        Scanner keyboard = new Scanner(System.in);
        while(true) {
            System.out.print("Take how many steps (0 to exit, default 1)? ");
            String n = keyboard.nextLine();
            int num = 1;
            try {
                num = Integer.parseInt(n);
            } catch (NumberFormatException ex) {}
            if (num <= 0) break;
            for(int i=0; i<num; i+=1) {
                simulator.cycle();
                simulator.showState();
            }
        }
        keyboard.close();
    }
}
