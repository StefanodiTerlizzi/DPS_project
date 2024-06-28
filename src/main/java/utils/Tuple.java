package utils;

import javax.xml.bind.annotation.XmlRootElement;

public class Tuple {
    @XmlRootElement
    public static class TwoTuple<A, B> {
        public final A one;
        public B two;

        public TwoTuple(A one, B two) {
            this.one = one;
            this.two = two;
        }

        @Override
        public String toString() {
            return "TwoTuple{" +
                    "one=" + one +
                    ", two=" + two +
                    '}';
        }
    }

    @XmlRootElement
    public static class ThreeTuple<A, B, C> {
        public final A one;
        public final B two;
        public final C three;

        public ThreeTuple(A one, B two, C three) {
            this.one = one;
            this.two = two;
            this.three = three;
        }

        @Override
        public String toString() {
            return "ThreeTuple{" +
                    "one=" + one +
                    ", two=" + two +
                    ", three=" + three +
                    '}';
        }
    }
}
