package sun.security.x509;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;

public class SubjectInfoAccessExtension extends Extension implements CertAttrSet<String> {
    public static final String DESCRIPTIONS = "descriptions";
    public static final String IDENT = "x509.info.extensions.SubjectInfoAccess";
    public static final String NAME = "SubjectInfoAccess";
    private List<AccessDescription> accessDescriptions;

    public SubjectInfoAccessExtension(List<AccessDescription> accessDescriptions) throws IOException {
        this.extensionId = PKIXExtensions.SubjectInfoAccess_Id;
        this.critical = false;
        this.accessDescriptions = accessDescriptions;
        encodeThis();
    }

    public SubjectInfoAccessExtension(Boolean critical, Object value) throws IOException {
        this.extensionId = PKIXExtensions.SubjectInfoAccess_Id;
        this.critical = critical.booleanValue();
        if (value instanceof byte[]) {
            this.extensionValue = (byte[]) value;
            DerValue val = new DerValue(this.extensionValue);
            if (val.tag == (byte) 48) {
                this.accessDescriptions = new ArrayList();
                while (val.data.available() != 0) {
                    this.accessDescriptions.add(new AccessDescription(val.data.getDerValue()));
                }
                return;
            }
            throw new IOException("Invalid encoding for SubjectInfoAccessExtension.");
        }
        throw new IOException("Illegal argument type");
    }

    public List<AccessDescription> getAccessDescriptions() {
        return this.accessDescriptions;
    }

    public String getName() {
        return NAME;
    }

    public void encode(OutputStream out) throws IOException {
        DerOutputStream tmp = new DerOutputStream();
        if (this.extensionValue == null) {
            this.extensionId = PKIXExtensions.SubjectInfoAccess_Id;
            this.critical = false;
            encodeThis();
        }
        super.encode(tmp);
        out.write(tmp.toByteArray());
    }

    public void set(String name, Object obj) throws IOException {
        if (!name.equalsIgnoreCase("descriptions")) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Attribute name [");
            stringBuilder.append(name);
            stringBuilder.append("] not recognized by CertAttrSet:SubjectInfoAccessExtension.");
            throw new IOException(stringBuilder.toString());
        } else if (obj instanceof List) {
            this.accessDescriptions = (List) obj;
            encodeThis();
        } else {
            throw new IOException("Attribute value should be of type List.");
        }
    }

    public List<AccessDescription> get(String name) throws IOException {
        if (name.equalsIgnoreCase("descriptions")) {
            return this.accessDescriptions;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Attribute name [");
        stringBuilder.append(name);
        stringBuilder.append("] not recognized by CertAttrSet:SubjectInfoAccessExtension.");
        throw new IOException(stringBuilder.toString());
    }

    public void delete(String name) throws IOException {
        if (name.equalsIgnoreCase("descriptions")) {
            this.accessDescriptions = Collections.emptyList();
            encodeThis();
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Attribute name [");
        stringBuilder.append(name);
        stringBuilder.append("] not recognized by CertAttrSet:SubjectInfoAccessExtension.");
        throw new IOException(stringBuilder.toString());
    }

    public Enumeration<String> getElements() {
        AttributeNameEnumeration elements = new AttributeNameEnumeration();
        elements.addElement("descriptions");
        return elements.elements();
    }

    private void encodeThis() throws IOException {
        if (this.accessDescriptions.isEmpty()) {
            this.extensionValue = null;
            return;
        }
        DerOutputStream ads = new DerOutputStream();
        for (AccessDescription accessDescription : this.accessDescriptions) {
            accessDescription.encode(ads);
        }
        DerOutputStream seq = new DerOutputStream();
        seq.write((byte) 48, ads);
        this.extensionValue = seq.toByteArray();
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(super.toString());
        stringBuilder.append("SubjectInfoAccess [\n  ");
        stringBuilder.append(this.accessDescriptions);
        stringBuilder.append("\n]\n");
        return stringBuilder.toString();
    }
}
