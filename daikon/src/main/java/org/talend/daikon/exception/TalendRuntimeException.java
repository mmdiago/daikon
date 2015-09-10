package org.talend.daikon.exception;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talend.daikon.exception.error.ErrorCode;
import org.talend.daikon.exception.json.JsonErrorCode;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Class for all business exception.
 */
public class TalendRuntimeException extends RuntimeException {

    private static final long         serialVersionUID = -5306654994356243153L;

    /** This class' logger. */
    private static final Logger       LOGGER           = LoggerFactory.getLogger(TalendRuntimeException.class);

    /** The error code for this exception. */
    private final ErrorCode           code;

    /** The exception cause. */
    private final Throwable           cause;

    /** Context of the error when it occurred (used to detail the user error message in frontend). */
    private final TalendExceptionContext context;

    /**
     * Full constructor.
     *
     * @param code the error code that holds all the .
     * @param cause the root cause of this error.
     * @param context the context of the error when it occurred (used to detail the user error message in frontend).
     */
    public TalendRuntimeException(ErrorCode code, Throwable cause, TalendExceptionContext context) {
        super(code.getCode(), cause);
        this.code = code;
        this.cause = cause;
        if (context == null) {
            this.context = TalendExceptionContext.build();
        } else {
            this.context = context;
        }

        checkContext();
    }

    /**
     * Lightweight constructor without context.
     *
     * @param code the error code that holds all the .
     * @param cause the root cause of this error.
     */
    public TalendRuntimeException(ErrorCode code, Throwable cause) {
        this(code, cause, null);
    }

    /**
     * Lightweight constructor without a cause.
     *
     * @param code the error code that holds all the .
     * @param context the exception context.
     */
    public TalendRuntimeException(ErrorCode code, TalendExceptionContext context) {
        this(code, null, context);
    }

    /**
     * Basic constructor from a JSON error code.
     *
     * @param code an error code serialized to JSON.
     */
    public TalendRuntimeException(JsonErrorCode code) {
        this(code, TalendExceptionContext.build().from(code.getContext()));
    }

    /**
     * Basic constructor with the bare error code.
     *
     * @param code the error code that holds all the .
     */
    public TalendRuntimeException(ErrorCode code) {
        this(code, null, null);
    }

    /**
     * Make sure that the context is filled with the expected context entries from the error code. If an entry is
     * missing, only a warning log is issued.
     */
    private void checkContext() {

        List<String> missingEntries = new ArrayList<>();

        for (String expectedEntry : code.getExpectedContextEntries()) {
            if (!context.contains(expectedEntry)) {
                missingEntries.add(expectedEntry);
            }
        }

        if (missingEntries.size() > 0) {
            LOGGER.warn("TDPException context for {}, is missing the given entry(ies) \n{}. \nStacktrace for info",
                    code.getCode(), missingEntries, this);
        }

    }

    /**
     * Describe this error in json into the given writer.
     * 
     * @param writer where to write this error.
     */
    void writeTo(Writer writer) {
        try {
            JsonGenerator generator = (new JsonFactory()).createGenerator(writer);
            generator.writeStartObject();
            {
                generator.writeStringField("code", code.getProduct() + '_' + code.getGroup() + '_' + code.getCode()); //$NON-NLS-1$
                generator.writeStringField("message", code.getCode()); //$NON-NLS-1$
                if (cause != null) {
                    generator.writeStringField("cause", cause.getMessage()); //$NON-NLS-1$
                }
                if (context != null) {
                    generator.writeFieldName("context"); //$NON-NLS-1$
                    generator.writeStartObject();
                    for (Map.Entry<String, Object> entry : context.entries()) {
                        generator.writeStringField(entry.getKey(), entry.getValue().toString());
                    }
                    generator.writeEndObject();
                }
            }
            generator.writeEndObject();
            generator.flush();
        } catch (IOException e) {
            LOGGER.error("Unable to write exception to " + writer + ".", e);
        }
    }

    /**
     * @return the error code.
     */
    public ErrorCode getCode() {
        return code;
    }
}
