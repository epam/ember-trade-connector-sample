package deltix.ember.connector.grpc.syneroex.util;

import com.epam.deltix.gflog.api.AppendableEntry;
import com.epam.deltix.gflog.api.Loggable;
import com.google.protobuf.*;
import deltix.anvil.util.AppendableEntryBuilder;
import deltix.anvil.util.Reusable;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public class SyneroexMessage implements Message, Loggable, Reusable {
    private Throwable throwable;
    private String message;

    public SyneroexMessage() {
        reuse();
    }

    public SyneroexMessage(String message, Throwable throwable) {
        this.throwable = throwable;
        this.message = message;
    }

    public SyneroexMessage error(Throwable throwable) {
        return this.message(throwable.getMessage()).throwable(throwable);
    }

    public Throwable throwable() {
        return throwable;
    }

    public SyneroexMessage throwable(Throwable throwable) {
        this.throwable = throwable;
        return this;
    }

    public SyneroexMessage message(String message) {
        this.message = message;
        return this;
    }

    public String message() {
        return message;
    }

    @Override
    public void reuse() {
        this.message = null;
        this.throwable = null;
    }

    @Override
    public void appendTo(AppendableEntry entry) {
        entry.append("{\"type\":\"").append(getClass().getSimpleName()).append("\", \"message\":\"").append(message).append("\"}");
    }

    @Override
    public boolean equals(Object other) {
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String toString() {
        AppendableEntryBuilder builder = new AppendableEntryBuilder(128);
        appendTo(builder);
        return builder.toString();
    }

    @Override
    public Parser<? extends Message> getParserForType() {
        return null;
    }

    @Override
    public Builder newBuilderForType() {
        return null;
    }

    @Override
    public Builder toBuilder() {
        return null;
    }

    @Override
    public void writeTo(CodedOutputStream output) throws IOException {

    }

    @Override
    public int getSerializedSize() {
        return 0;
    }

    @Override
    public ByteString toByteString() {
        return null;
    }

    @Override
    public byte[] toByteArray() {
        return new byte[0];
    }

    @Override
    public void writeTo(OutputStream output) throws IOException {

    }

    @Override
    public void writeDelimitedTo(OutputStream output) throws IOException {

    }

    @Override
    public Message getDefaultInstanceForType() {
        return null;
    }

    @Override
    public List<String> findInitializationErrors() {
        return null;
    }

    @Override
    public String getInitializationErrorString() {
        return null;
    }

    @Override
    public Descriptors.Descriptor getDescriptorForType() {
        return null;
    }

    @Override
    public Map<Descriptors.FieldDescriptor, Object> getAllFields() {
        return null;
    }

    @Override
    public boolean hasOneof(Descriptors.OneofDescriptor oneof) {
        return false;
    }

    @Override
    public Descriptors.FieldDescriptor getOneofFieldDescriptor(Descriptors.OneofDescriptor oneof) {
        return null;
    }

    @Override
    public boolean hasField(Descriptors.FieldDescriptor field) {
        return false;
    }

    @Override
    public Object getField(Descriptors.FieldDescriptor field) {
        return null;
    }

    @Override
    public int getRepeatedFieldCount(Descriptors.FieldDescriptor field) {
        return 0;
    }

    @Override
    public Object getRepeatedField(Descriptors.FieldDescriptor field, int index) {
        return null;
    }

    @Override
    public UnknownFieldSet getUnknownFields() {
        return null;
    }

    @Override
    public boolean isInitialized() {
        return true;
    }
}
