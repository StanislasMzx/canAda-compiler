package asm;

public class RegisterManager {
    private final boolean[] registers;
    private int highestUsedRegister;

    public RegisterManager() {
        registers = new boolean[10];
        highestUsedRegister = -1;
    }

    public int borrowRegister() {
        for (int i = 0; i < registers.length; i++) {
            if (!registers[i]) {
                registers[i] = true;
                highestUsedRegister = Math.max(highestUsedRegister, i);
                return i;
            }
        }
        throw new RuntimeException("No more registers available");
    }

    public void freeRegister(int register) {
        registers[register] = false;
    }

    public String generateStmfd() {
        StringBuilder stmfd = new StringBuilder();
        stmfd.append("r13!, {");
        for (int i = 0; i <= highestUsedRegister; i++) {
            stmfd.append("r").append(i).append(", ");
        }
        stmfd.append("lr}");
        return stmfd.toString();
    }

    public String generateLdmfd() {
        StringBuilder ldmfd = new StringBuilder();
        ldmfd.append("r13!, {");
        for (int i = 0; i <= highestUsedRegister; i++) {
            ldmfd.append("r").append(i).append(", ");
        }
        ldmfd.append("pc}");
        return ldmfd.toString();
    }
}
