import com.kingmang.ixion.Ixion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class ExamplesTest {

    @Test
    void adt() {
        ixAssert("adt.ix", """
                value 10 is integer
                value 10.0 is float
                """);
    }

    @Test
    void generics(){
        ixAssert("generics.ix", """
                Hello
                10
                """);
    }

    @Test
    void loops(){
        ixAssert("loops.ix", """
                1
                2
                3
                4
                5
                ----
                i is 10
                i is 11
                i is 12
                i is 13
                i is 14
                i is 15
                i is 16
                i is 17
                i is 18
                i is 19
                """);
    }

    @Test
    void simple_list(){
        ixAssert("simple_list.ix", """
                [1, 2, 3]
                [20]
                """);
    }

    @Test
    void struct(){
        ixAssert("struct.ix", """
                value{left{first}, right{second}}
                """);
    }

    void ixAssert(String runPath, String resPath) {
        Ixion api = new Ixion();
        assertDoesNotThrow(() -> assertEquals(resPath, api.getCompiledProgramOutput("/src/test/resources/" + runPath)));
    }

}