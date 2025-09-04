package uy.com.bay.utiles.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class FormattingUtils {

    private static final DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
    private static final DecimalFormat formatter;

    static {
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('.');
        
        formatter = new DecimalFormat("#,##0.00", symbols);
    }

    public static String formatAmount(double amount) {
        return formatter.format(amount);
    }
}
