package com.sohu.smc;

import com.sohu.smc.anno.MdCacheable;
import org.junit.Test;

import java.lang.reflect.Method;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/17
 */
public class Person {

    @MdCacheable(key = "2123")
    public void fun(){

    }

    @Test
    public void fun1() throws NoSuchMethodException {
        Method fun = this.getClass().getMethod("fun");
        MdCacheable annotation = fun.getAnnotation(MdCacheable.class);
        String key = annotation.key();
        System.out.println(key);

    }

}
