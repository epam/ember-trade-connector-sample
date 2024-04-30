package deltix.ember.connector.grpc.syneroex.session;

import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import deltix.anvil.util.AppendableEntryBuilder;
import deltix.anvil.util.LangUtil;
import deltix.anvil.util.buffer.MutableBuffer;
import deltix.anvil.util.buffer.UnsafeBuffer;
import deltix.ember.connector.grpc.syneroex.SyneroexUtil;

import java.io.IOException;

public class MessageFormatter {
    private final MutableBuffer buffer = new UnsafeBuffer();
    private final AppendableEntryBuilder builder = new AppendableEntryBuilder(16 * 1024);
    private final JsonFormat.Printer printer = SyneroexUtil.createJsonPrinter();

    public MutableBuffer buffer() {
        return buffer;
    }

    public int format(boolean inbound, Message message) {
        try {
            builder.clear();

            builder.append(inbound ? "[INB] " : "[OUT] ")
                    .append("{\"type\":\"").append(message.getClass().getSimpleName())
                    .append("\",\"message\":");

            printer.appendTo(message, builder);
            builder.append('}');

            buffer.wrap(builder.builder().array(), 0, builder.length());
            return builder.length();
        } catch (IOException e) {
            throw LangUtil.rethrowUnchecked(e);
        }
    }
}
