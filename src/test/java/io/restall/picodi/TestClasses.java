package io.restall.picodi;

/**
 * Classes used in tests
 */
public class TestClasses {

    public static class NoArg {

    }

    public static class A {

    }

    public static class B {
        private final A a;

        public B(A a) {
            this.a = a;
            // one arg constructor with A
        }

        public A getA() {
            return a;
        }
    }

    public static class C {
        public C(String c) {

        }

        public C(int c) {

        }
    }

    public static class D {
        private static int count = 0;

        public D() {
            count++;
        }

        public static int getCount() {
            return count;
        }

        public static void reset() {
            count = 0;
        }
    }

    public static class E {
        private final D d;

        public E(D d) {
            this.d = d;
        }

        public D getD() {
            return d;
        }
    }

    public static class F {
        private final G g;

        public F(G g) {
            this.g = g;
        }

        public G getG() {
            return g;
        }
    }

    public static class G {
        private final H h;

        public G(H h) {
            this.h = h;
        }

        public H getH() {
            return h;
        }
    }

    public static class H {
        private final F f;

        public H(F f) {
            this.f = f;
        }

        public F getF() {
            return f;
        }
    }

    public static class I {
        private final G g;

        public I(G g) {
            this.g = g;
        }

        public G getG() {
            return g;
        }
    }

    public interface J {

    }

    public static class K implements J {

    }

    public static class L {

    }

    public static class M extends L {

    }

    public static abstract class N {

    }

    public static class O extends N {

    }

    public static class Q {
        private final A a;

        public Q(String str) {
            this.a = null;
        }

        @PrimaryConstructor
        public Q(A a) {
            this.a = a;
        }

        public A getA() {
            return a;
        }
    }
}
