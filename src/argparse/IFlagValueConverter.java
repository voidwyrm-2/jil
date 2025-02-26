package argparse;

public interface IFlagValueConverter<T> {
    T convert(String arg) throws Exception;
}
