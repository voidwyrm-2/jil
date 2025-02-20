import java.util.Arrays;
import java.util.HashMap;

public class JILMemory {
    int[] memory;
    HashMap<Integer, Integer> allocations;

    public JILMemory(int size) {
        memory = new int[size];
        allocations = new HashMap<>();
    }

    private void upsize(int amount) {
        int[] newMemory = new int[memory.length + amount];
        System.arraycopy(memory, 0, newMemory, 0, memory.length);
        memory = newMemory;
    }

    private void downsize(int amount) throws JILException {
        if (memory.length - amount < 0) throw new JILException("cannot downsize memory below zero");
        memory = Arrays.copyOf(memory, memory.length - amount);
    }

    public int malloc(int size) {
        throw new UnsupportedOperationException("not implemented");
    }

    public int free(int address) {
        throw new UnsupportedOperationException("not implemented");
    }
}
