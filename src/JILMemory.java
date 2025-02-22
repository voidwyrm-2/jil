import java.util.Arrays;
import java.util.HashMap;

public class JILMemory {
    private int[] memory;
    private HashMap<Integer, Allocation> allocations;
    private int allocatedSpace;

    public MemoryDebug debug;

    private record Allocation(int start, int size) implements Comparable<AllocationEntry> {
        int end() {
            return start + size;
        }

        @Override
        public int compareTo(AllocationEntry o) {
            return Integer.compare(start, o.start);
        }
    }

    private record AllocationEntry(int id, int start, int size) implements Comparable<AllocationEntry> {
        Allocation toAllocation() {
            return new Allocation(start, size);
        }

        @Override
        public int compareTo(AllocationEntry o) {
            return Integer.compare(start, o.start);
        }

        @Override
        public String toString() {
            return String.format("<id: %d, start: %d, size: %d>", id, start, size);
        }
    }

    public record MemoryDebug(JILMemory memref) {
        public void printMemory() {
            System.out.println("memory: " + Arrays.toString(memref.memory));
        }

        public void printAllocations() {
            StringBuilder str = new StringBuilder();

            str.append("total allocated space: ").append(memref.allocatedSpace).append(" of ").append(memref.memory.length).append("\n");

            AllocationEntry[] entries = new AllocationEntry[memref.allocations.size()];
            {
                int i = 0;
                for (Integer k : memref.allocations.keySet()) {
                    Allocation a = memref.allocations.get(k);
                    entries[i] = new AllocationEntry(k, a.start, a.size);
                    i++;
                }
            }

            Arrays.sort(entries);
            for (AllocationEntry e : entries) str.append(e).append("\n");

            System.out.print(str);
        }
    }

    public JILMemory(int size) throws JILException {
        memory = new int[size];
        allocations = new HashMap<>();
        allocatedSpace = 0;

        debug = new MemoryDebug(this);
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

        allocations.put(ptrID, new Allocation(allocatedSpace, size));
        allocatedSpace += size;

        return ptrID;
    }

    public void free(int id) throws JILException {
        checkID(id);

        Allocation freedAlloc = allocations.remove(id);
        allocatedSpace -= freedAlloc.size;

        clear(freedAlloc.start, freedAlloc.end());

        AllocationEntry[] existingAllocs = new AllocationEntry[allocations.size()];
        {
            int i = 0;
            for (Integer k : allocations.keySet()) {
                Allocation a = allocations.get(k);
                existingAllocs[i] = new AllocationEntry(k, a.start, a.size);
                i++;
            }
        }

        Arrays.sort(existingAllocs);

        int prevEnd = freedAlloc.end();
        for (AllocationEntry ae : existingAllocs) {
            if (ae.start > prevEnd) {
                if (ae.start - prevEnd == 1)
                    continue;

                Allocation oldAlloc = ae.toAllocation();

                int newStart = ae.start - prevEnd;
                prevEnd = oldAlloc.end();

                if (oldAlloc.size == 1) {
                    int prevValue = memory[oldAlloc.start];
                    memory[oldAlloc.start] = 0;
                    memory[newStart] = prevValue;
                } else {
                    int[] prevArr = Arrays.copyOfRange(memory, oldAlloc.start, oldAlloc.end());
                    clear(oldAlloc.start, oldAlloc.end());
                    System.arraycopy(prevArr, 0, memory, newStart, prevArr.length);
                }

                allocations.put(ae.id, new Allocation(newStart, oldAlloc.size));
            }
        }
    }

    public void deref(int id, int value) throws JILException {
        checkID(id);
        memory[allocations.get(id).start] = value;
    }

    public void derefArray(int id, int[] arr) throws JILException {
        checkID(id);
        Allocation a = allocations.get(id);
        for (int i = 0; i < a.size && i < arr.length; i++) memory[a.start + i] = arr[i];
    }

    public void derefString(int id, String str) throws JILException {
        checkID(id);
        Allocation a = allocations.get(id);
        byte[] bytes = str.getBytes();
        for (int i = 0; i < a.size && i < str.length(); i++) memory[a.start + i] = bytes[i];
    }

    public int deref(int id) throws JILException {
        checkID(id);
        return memory[allocations.get(id).start];
    }

    public int[] derefArray(int id) throws JILException {
        checkID(id);
        Allocation a = allocations.get(id);
        return Arrays.copyOfRange(memory, a.start, a.end());
    }

    public String derefString(int id) throws JILException {
        checkID(id);
        Allocation a = allocations.get(id);
        StringBuilder str = new StringBuilder();
        for (int n : Arrays.copyOfRange(memory, a.start, a.end())) str.append((char)n);
        return str.toString();
    }
}