package ixion.std;

public class array<T> {
    private final T[] elements;
    private int size;
    private final int capacity;

    @SuppressWarnings("unchecked")
    public array(int capacity) {
        this.capacity = capacity;
        this.elements = (T[]) new Object[capacity];
        this.size = 0;
    }

    public void add(T element) {
        if (size >= capacity) {
            throw new ArrayIndexOutOfBoundsException("Array is full");
        }
        elements[size++] = element;
    }

    public T get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index out of bounds");
        }
        return elements[index];
    }

    public int size() {
        return size;
    }

}
