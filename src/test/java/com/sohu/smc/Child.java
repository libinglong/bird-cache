package com.sohu.smc;

import com.sohu.smc.anno.MdCacheable;
import org.junit.Test;

import java.lang.reflect.Method;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/17
 */
public class Child extends Person {

    @MdCacheable(key = "12")
    public void fun(){

    }


    @Test
    public void fun1() throws NoSuchMethodException {
        Method fun = Child.class.getMethod("fun");
        Method fun1 = Person.class.getMethod("fun");
        System.out.println();
        MdCacheable annotation = fun.getAnnotation(MdCacheable.class);
        String key = annotation.key();
        System.out.println(key);

    }

}
