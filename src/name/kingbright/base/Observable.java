package name.kingbright.base;

public interface Observable<T> {
    public void addObserver(Observer<T> obs);

    public void removeObserver(Observer<T> obs);
}
