package com.company.Functions;

import com.company.Utils.DataFormat;
import com.company.Utils.PrimitiveType;

import java.io.IOException;

public interface SlidingWindow {
    public static final PrimitiveType primitiveType = null;
    public void goBackN(DataFormat dataFormat) throws IOException;
    public void selectiveRepeat();
}
