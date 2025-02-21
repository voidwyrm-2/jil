import java.util.Arrays;
import java.util.HashMap;

public class JILMemory {
    int[] memory;
    HashMap<Integer, Allocation> allocations;
    int allocatedSpace;

    private record Allocation(int start, int size) {
        int end() {
            return start + size;
        }
    }

    public JILMemory(int size) throws JILException {
        memory = new int[size];
        allocations = new HashMap<>();
        allocatedSpace = 0;
    }

    private void upsize(int amount) throws JILException {
        int[] newMemory = new int[memory.length + amount];
        System.arraycopy(memory, 0, newMemory, 0, memory.length);
        memory = newMemory;
    }

    private void downsize(int amount) throws JILException {
        if (memory.length - amount < 0)
            throw new JILException("cannot downsize memory below zero");
        memory = Arrays.copyOf(memory, memory.length - amount);
    }

    private void clear(int start, int end) {
        for (int i = start; i != end; i++) memory[i] = 0;
    }

    private int generateID() {
        double dFirst = Math.random();
        double dSecond = Math.random();
        double dThird = Math.random();

        int first = (int)(dFirst * 100000);
        int second = (int)(dSecond * 100000);
        int third = (int)(dThird * 100000);

        return (((first << second) / third) * first) / (third & first);
    }

    private void checkID(int id) throws JILException {
        if (!allocations.containsKey(id))
            throw new JILException(String.format("%d is not an allocation", id));
    }

    public int malloc(int size) throws JILException {
        if (size < 1)
            throw new JILException("size cannot be less than one");
        else if (memory.length - allocatedSpace < size)
            //upsize(memory.length - allocatedSpace - size);
            throw new JILException("out of memory for allocation");

        int ptrID = generateID();

        while (allocations.containsKey(ptrID))
            ptrID = generateID();

        allocations.put(ptrID, new Allocation(allocatedSpace == 0 ? 0 : allocatedSpace + 1, size));
        allocatedSpace += size;

        return ptrID;
    }

    public void free(int id) throws JILException {
        checkID(id);

        Allocation alloc = allocations.remove(id);
        allocatedSpace -= alloc.size;

        clear(alloc.start, alloc.end());

        for (int k : allocations.keySet()) {
            Allocation a = allocations.remove(k);
            if (a.start > id)
                allocations.put(k, new Allocation(a.start - alloc.size, a.size));
        }
    }

    public void setRef(int id) throws JILException {
        checkID(id);
        throw new UnsupportedOperationException("not implemented");
    }

    public void setArrayRef(int id, int[] arr) throws JILException {
        checkID(id);
        throw new UnsupportedOperationException("not implemented");
    }

    public int getRef(int id) throws JILException {
        checkID(id);
        return memory[allocations.get(id).start];
    }

    public int[] getArrayRef(int id) throws JILException {
        checkID(id);
        Allocation a = allocations.get(id);
        return Arrays.copyOfRange(memory, a.start, a.end());
    }
}
