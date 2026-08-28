public class Test {
    public static void main(String[] args) {
        float nan = Float.NaN;
        float min = 0f;
        float max = 1f;
        float res = nan < min ? min : (nan > max ? max : nan);
        System.out.println(res);
    }
}
