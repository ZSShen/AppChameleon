package org.zsshen.simpleproxy;


import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectionWrapper {

    public static Object invokeStaticMethod(String szClass, String szMethod, Class[] aParaType, Object[] aParaValue)
    {
        Object objRtn = null;
        try {
            Class clsTarget = Class.forName(szClass);
            Method metTarget = clsTarget.getMethod(szMethod, aParaType);
            objRtn = metTarget.invoke(null, aParaValue);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return objRtn;
    }

    public static Object invokeMethod(String szClass, String szMethod, Object objClass, Class[] aParaType, Object[] aParaValue)
    {
        Object objRtn = null;
        try {
            Class clsTarget = Class.forName(szClass);
            Method metTarget = clsTarget.getMethod(szMethod, aParaType);
            objRtn = metTarget.invoke(objClass, aParaValue);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return objRtn;
    }

    public static Object getStaticFieldObject(String szClass, String szField)
    {
        Object objField = null;
        try {
            Class clsTarget = Class.forName(szClass);
            Field fidTarget = clsTarget.getDeclaredField(szField);
            fidTarget.setAccessible(true);
            objField = fidTarget.get(null);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return objField;
    }

    public static Object getFieldObject(String szClass, Object objClass, String szField)
    {
        Object objField = null;
        try {
            Class clsTarget = Class.forName(szClass);
            Field fidTarget = clsTarget.getDeclaredField(szField);
            fidTarget.setAccessible(true);
            objField = fidTarget.get(objClass);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return objField;
    }

    public static void setStaticFieldObject(String szClass, String szField, Object objField)
    {
        try {
            Class clsTarget = Class.forName(szClass);
            Field fidTarget = clsTarget.getDeclaredField(szField);
            fidTarget.setAccessible(true);
            fidTarget.set(null, objField);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return;
    }

    public static void setFieldObject(String szClass, Object objClass, String szField, Object objField)
    {
        try {
            Class clsTarget = Class.forName(szClass);
            Field fidTarget = clsTarget.getDeclaredField(szField);
            fidTarget.setAccessible(true);
            fidTarget.set(objClass, objField);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return;
    }
}
