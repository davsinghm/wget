package com.davsinghm.wget.core.util;

import java.util.LinkedList;
import java.util.List;

import androidx.annotation.NonNull;

public class ExceptionUtils {

    @NonNull
    public static String getThrowableCauseMessage(@NonNull Throwable throwable) {
        List<String> msgs = new LinkedList<>();

        List<Throwable> list = getThrowableList(throwable);
        for (int i = 0; i < list.size(); i++) {
            Throwable t = list.get(i);
            String message = throwableToString(t);

            String nextToString = i < list.size() - 1 ? throwableToString(list.get(i + 1)) : null;
            if (nextToString != null && nextToString.equals(t.getMessage()))
                message = t.getClass().getName(); //ignore the message, as it was toString() by default constructor

            msgs.add(message);
        }

        return StringUtil.join("\n    by: ", msgs);
    }

    private static String throwableToString(@NonNull Throwable t) {
        String s = t.getClass().getName();
        String message = t.getMessage();
        message = (message != null) ? (s + ": " + message) : s;
        return message;
    }

    private static List<Throwable> getThrowableList(@NonNull Throwable throwable) {
        List<Throwable> list = new LinkedList<>();
        while (throwable != null && !list.contains(throwable)) {
            list.add(throwable);
            throwable = throwable.getCause();
        }

        return list;
    }

    public static boolean areThrowableSame(@NonNull Throwable t1, @NonNull Throwable t2) {
        List<Throwable> list1 = getThrowableList(t1);
        List<Throwable> list2 = getThrowableList(t2);
        int size = list1.size();
        if (size == 0 || list1.size() != list2.size())
            return false;

        for (int i = 0; i < size; i++)
            if (!compareThrowable(list1.get(i), list2.get(i)))
                return false;

        return true;
    }

    private static boolean compareThrowable(@NonNull Throwable t1, @NonNull Throwable t2) {
        if (t1 == t2)
            return true;

        if (t1.getClass() != t2.getClass())
            return false;

        String msg1 = t1.getMessage();
        String msg2 = t2.getMessage();
        String lMsg1 = t1.getLocalizedMessage();
        String lMsg2 = t2.getLocalizedMessage();
        return ((msg1 == null && msg2 == null) || (msg1 != null && msg1.equals(msg2)))
                && ((lMsg1 == null && lMsg2 == null) || (lMsg1 != null && lMsg1.equals(lMsg2)));
    }

}
