package jcifs.dcerpc.msrpc;

import jcifs.dcerpc.DcerpcMessage;
import jcifs.dcerpc.ndr.NdrBuffer;
import jcifs.dcerpc.ndr.NdrException;
import jcifs.dcerpc.ndr.NdrObject;
import jcifs.dcerpc.ndr.NdrSmall;
import jcifs.dcerpc.rpc;

public class lsarpc {
    public static final int POLICY_INFO_ACCOUNT_DOMAIN = 5;
    public static final int POLICY_INFO_AUDIT_EVENTS = 2;
    public static final int POLICY_INFO_DNS_DOMAIN = 12;
    public static final int POLICY_INFO_MODIFICATION = 9;
    public static final int POLICY_INFO_PRIMARY_DOMAIN = 3;
    public static final int POLICY_INFO_SERVER_ROLE = 6;
    public static final int SID_NAME_ALIAS = 4;
    public static final int SID_NAME_DELETED = 6;
    public static final int SID_NAME_DOMAIN = 3;
    public static final int SID_NAME_DOM_GRP = 2;
    public static final int SID_NAME_INVALID = 7;
    public static final int SID_NAME_UNKNOWN = 8;
    public static final int SID_NAME_USER = 1;
    public static final int SID_NAME_USE_NONE = 0;
    public static final int SID_NAME_WKN_GRP = 5;

    public static String getSyntax() {
        return "12345778-1234-abcd-ef00-0123456789ab:0.0";
    }

    public static class LsarQosInfo extends NdrObject {
        public byte context_mode;
        public byte effective_only;
        public short impersonation_level;
        public int length;

        @Override // jcifs.dcerpc.ndr.NdrObject
        public void encode(NdrBuffer _dst) throws NdrException {
            _dst.align(4);
            _dst.enc_ndr_long(this.length);
            _dst.enc_ndr_short(this.impersonation_level);
            _dst.enc_ndr_small(this.context_mode);
            _dst.enc_ndr_small(this.effective_only);
        }

        @Override // jcifs.dcerpc.ndr.NdrObject
        public void decode(NdrBuffer _src) throws NdrException {
            _src.align(4);
            this.length = _src.dec_ndr_long();
            this.impersonation_level = (short) _src.dec_ndr_short();
            this.context_mode = (byte) _src.dec_ndr_small();
            this.effective_only = (byte) _src.dec_ndr_small();
        }
    }

    public static class LsarObjectAttributes extends NdrObject {
        public int attributes;
        public int length;
        public rpc.unicode_string object_name;
        public NdrSmall root_directory;
        public int security_descriptor;
        public LsarQosInfo security_quality_of_service;

        @Override // jcifs.dcerpc.ndr.NdrObject
        public void encode(NdrBuffer _dst) throws NdrException {
            _dst.align(4);
            _dst.enc_ndr_long(this.length);
            _dst.enc_ndr_referent(this.root_directory, 1);
            _dst.enc_ndr_referent(this.object_name, 1);
            _dst.enc_ndr_long(this.attributes);
            _dst.enc_ndr_long(this.security_descriptor);
            _dst.enc_ndr_referent(this.security_quality_of_service, 1);
            if (this.root_directory != null) {
                _dst = _dst.deferred;
                this.root_directory.encode(_dst);
            }
            if (this.object_name != null) {
                _dst = _dst.deferred;
                this.object_name.encode(_dst);
            }
            if (this.security_quality_of_service != null) {
                this.security_quality_of_service.encode(_dst.deferred);
            }
        }

        @Override // jcifs.dcerpc.ndr.NdrObject
        public void decode(NdrBuffer _src) throws NdrException {
            _src.align(4);
            this.length = _src.dec_ndr_long();
            int _root_directoryp = _src.dec_ndr_long();
            int _object_namep = _src.dec_ndr_long();
            this.attributes = _src.dec_ndr_long();
            this.security_descriptor = _src.dec_ndr_long();
            int _security_quality_of_servicep = _src.dec_ndr_long();
            if (_root_directoryp != 0) {
                _src = _src.deferred;
                this.root_directory.decode(_src);
            }
            if (_object_namep != 0) {
                if (this.object_name == null) {
                    this.object_name = new rpc.unicode_string();
                }
                _src = _src.deferred;
                this.object_name.decode(_src);
            }
            if (_security_quality_of_servicep != 0) {
                if (this.security_quality_of_service == null) {
                    this.security_quality_of_service = new LsarQosInfo();
                }
                this.security_quality_of_service.decode(_src.deferred);
            }
        }
    }

    public static class LsarDomainInfo extends NdrObject {
        public rpc.unicode_string name;
        public rpc.sid_t sid;

        @Override // jcifs.dcerpc.ndr.NdrObject
        public void encode(NdrBuffer _dst) throws NdrException {
            _dst.align(4);
            _dst.enc_ndr_short(this.name.length);
            _dst.enc_ndr_short(this.name.maximum_length);
            _dst.enc_ndr_referent(this.name.buffer, 1);
            _dst.enc_ndr_referent(this.sid, 1);
            if (this.name.buffer != null) {
                NdrBuffer _dst2 = _dst.deferred;
                int _name_bufferl = this.name.length / 2;
                _dst2.enc_ndr_long(this.name.maximum_length / 2);
                _dst2.enc_ndr_long(0);
                _dst2.enc_ndr_long(_name_bufferl);
                int _name_bufferi = _dst2.index;
                _dst2.advance(_name_bufferl * 2);
                _dst = _dst2.derive(_name_bufferi);
                for (int _i = 0; _i < _name_bufferl; _i++) {
                    _dst.enc_ndr_short(this.name.buffer[_i]);
                }
            }
            if (this.sid != null) {
                this.sid.encode(_dst.deferred);
            }
        }

        @Override // jcifs.dcerpc.ndr.NdrObject
        public void decode(NdrBuffer _src) throws NdrException {
            _src.align(4);
            _src.align(4);
            if (this.name == null) {
                this.name = new rpc.unicode_string();
            }
            this.name.length = (short) _src.dec_ndr_short();
            this.name.maximum_length = (short) _src.dec_ndr_short();
            int _name_bufferp = _src.dec_ndr_long();
            int _sidp = _src.dec_ndr_long();
            if (_name_bufferp != 0) {
                NdrBuffer _src2 = _src.deferred;
                int _name_buffers = _src2.dec_ndr_long();
                _src2.dec_ndr_long();
                int _name_bufferl = _src2.dec_ndr_long();
                int _name_bufferi = _src2.index;
                _src2.advance(_name_bufferl * 2);
                if (this.name.buffer == null) {
                    if (_name_buffers < 0 || _name_buffers > 65535) {
                        throw new NdrException(NdrException.INVALID_CONFORMANCE);
                    }
                    this.name.buffer = new short[_name_buffers];
                }
                _src = _src2.derive(_name_bufferi);
                for (int _i = 0; _i < _name_bufferl; _i++) {
                    this.name.buffer[_i] = (short) _src.dec_ndr_short();
                }
            }
            if (_sidp != 0) {
                if (this.sid == null) {
                    this.sid = new rpc.sid_t();
                }
                this.sid.decode(_src.deferred);
            }
        }
    }

    public static class LsarDnsDomainInfo extends NdrObject {
        public rpc.unicode_string dns_domain;
        public rpc.unicode_string dns_forest;
        public rpc.uuid_t domain_guid;
        public rpc.unicode_string name;
        public rpc.sid_t sid;

        @Override // jcifs.dcerpc.ndr.NdrObject
        public void encode(NdrBuffer _dst) throws NdrException {
            _dst.align(4);
            _dst.enc_ndr_short(this.name.length);
            _dst.enc_ndr_short(this.name.maximum_length);
            _dst.enc_ndr_referent(this.name.buffer, 1);
            _dst.enc_ndr_short(this.dns_domain.length);
            _dst.enc_ndr_short(this.dns_domain.maximum_length);
            _dst.enc_ndr_referent(this.dns_domain.buffer, 1);
            _dst.enc_ndr_short(this.dns_forest.length);
            _dst.enc_ndr_short(this.dns_forest.maximum_length);
            _dst.enc_ndr_referent(this.dns_forest.buffer, 1);
            _dst.enc_ndr_long(this.domain_guid.time_low);
            _dst.enc_ndr_short(this.domain_guid.time_mid);
            _dst.enc_ndr_short(this.domain_guid.time_hi_and_version);
            _dst.enc_ndr_small(this.domain_guid.clock_seq_hi_and_reserved);
            _dst.enc_ndr_small(this.domain_guid.clock_seq_low);
            int _domain_guid_nodei = _dst.index;
            _dst.advance(6);
            _dst.enc_ndr_referent(this.sid, 1);
            if (this.name.buffer != null) {
                NdrBuffer _dst2 = _dst.deferred;
                int _name_bufferl = this.name.length / 2;
                _dst2.enc_ndr_long(this.name.maximum_length / 2);
                _dst2.enc_ndr_long(0);
                _dst2.enc_ndr_long(_name_bufferl);
                int _name_bufferi = _dst2.index;
                _dst2.advance(_name_bufferl * 2);
                _dst = _dst2.derive(_name_bufferi);
                for (int _i = 0; _i < _name_bufferl; _i++) {
                    _dst.enc_ndr_short(this.name.buffer[_i]);
                }
            }
            if (this.dns_domain.buffer != null) {
                NdrBuffer _dst3 = _dst.deferred;
                int _dns_domain_bufferl = this.dns_domain.length / 2;
                _dst3.enc_ndr_long(this.dns_domain.maximum_length / 2);
                _dst3.enc_ndr_long(0);
                _dst3.enc_ndr_long(_dns_domain_bufferl);
                int _dns_domain_bufferi = _dst3.index;
                _dst3.advance(_dns_domain_bufferl * 2);
                _dst = _dst3.derive(_dns_domain_bufferi);
                for (int _i2 = 0; _i2 < _dns_domain_bufferl; _i2++) {
                    _dst.enc_ndr_short(this.dns_domain.buffer[_i2]);
                }
            }
            if (this.dns_forest.buffer != null) {
                NdrBuffer _dst4 = _dst.deferred;
                int _dns_forest_bufferl = this.dns_forest.length / 2;
                _dst4.enc_ndr_long(this.dns_forest.maximum_length / 2);
                _dst4.enc_ndr_long(0);
                _dst4.enc_ndr_long(_dns_forest_bufferl);
                int _dns_forest_bufferi = _dst4.index;
                _dst4.advance(_dns_forest_bufferl * 2);
                _dst = _dst4.derive(_dns_forest_bufferi);
                for (int _i3 = 0; _i3 < _dns_forest_bufferl; _i3++) {
                    _dst.enc_ndr_short(this.dns_forest.buffer[_i3]);
                }
            }
            NdrBuffer _dst5 = _dst.derive(_domain_guid_nodei);
            for (int _i4 = 0; _i4 < 6; _i4++) {
                _dst5.enc_ndr_small(this.domain_guid.node[_i4]);
            }
            if (this.sid != null) {
                this.sid.encode(_dst5.deferred);
            }
        }

        @Override // jcifs.dcerpc.ndr.NdrObject
        public void decode(NdrBuffer _src) throws NdrException {
            _src.align(4);
            _src.align(4);
            if (this.name == null) {
                this.name = new rpc.unicode_string();
            }
            this.name.length = (short) _src.dec_ndr_short();
            this.name.maximum_length = (short) _src.dec_ndr_short();
            int _name_bufferp = _src.dec_ndr_long();
            _src.align(4);
            if (this.dns_domain == null) {
                this.dns_domain = new rpc.unicode_string();
            }
            this.dns_domain.length = (short) _src.dec_ndr_short();
            this.dns_domain.maximum_length = (short) _src.dec_ndr_short();
            int _dns_domain_bufferp = _src.dec_ndr_long();
            _src.align(4);
            if (this.dns_forest == null) {
                this.dns_forest = new rpc.unicode_string();
            }
            this.dns_forest.length = (short) _src.dec_ndr_short();
            this.dns_forest.maximum_length = (short) _src.dec_ndr_short();
            int _dns_forest_bufferp = _src.dec_ndr_long();
            _src.align(4);
            if (this.domain_guid == null) {
                this.domain_guid = new rpc.uuid_t();
            }
            this.domain_guid.time_low = _src.dec_ndr_long();
            this.domain_guid.time_mid = (short) _src.dec_ndr_short();
            this.domain_guid.time_hi_and_version = (short) _src.dec_ndr_short();
            this.domain_guid.clock_seq_hi_and_reserved = (byte) _src.dec_ndr_small();
            this.domain_guid.clock_seq_low = (byte) _src.dec_ndr_small();
            int _domain_guid_nodei = _src.index;
            _src.advance(6);
            int _sidp = _src.dec_ndr_long();
            if (_name_bufferp != 0) {
                NdrBuffer _src2 = _src.deferred;
                int _name_buffers = _src2.dec_ndr_long();
                _src2.dec_ndr_long();
                int _name_bufferl = _src2.dec_ndr_long();
                int _name_bufferi = _src2.index;
                _src2.advance(_name_bufferl * 2);
                if (this.name.buffer == null) {
                    if (_name_buffers < 0 || _name_buffers > 65535) {
                        throw new NdrException(NdrException.INVALID_CONFORMANCE);
                    }
                    this.name.buffer = new short[_name_buffers];
                }
                _src = _src2.derive(_name_bufferi);
                for (int _i = 0; _i < _name_bufferl; _i++) {
                    this.name.buffer[_i] = (short) _src.dec_ndr_short();
                }
            }
            if (_dns_domain_bufferp != 0) {
                NdrBuffer _src3 = _src.deferred;
                int _dns_domain_buffers = _src3.dec_ndr_long();
                _src3.dec_ndr_long();
                int _dns_domain_bufferl = _src3.dec_ndr_long();
                int _dns_domain_bufferi = _src3.index;
                _src3.advance(_dns_domain_bufferl * 2);
                if (this.dns_domain.buffer == null) {
                    if (_dns_domain_buffers < 0 || _dns_domain_buffers > 65535) {
                        throw new NdrException(NdrException.INVALID_CONFORMANCE);
                    }
                    this.dns_domain.buffer = new short[_dns_domain_buffers];
                }
                _src = _src3.derive(_dns_domain_bufferi);
                for (int _i2 = 0; _i2 < _dns_domain_bufferl; _i2++) {
                    this.dns_domain.buffer[_i2] = (short) _src.dec_ndr_short();
                }
            }
            if (_dns_forest_bufferp != 0) {
                NdrBuffer _src4 = _src.deferred;
                int _dns_forest_buffers = _src4.dec_ndr_long();
                _src4.dec_ndr_long();
                int _dns_forest_bufferl = _src4.dec_ndr_long();
                int _dns_forest_bufferi = _src4.index;
                _src4.advance(_dns_forest_bufferl * 2);
                if (this.dns_forest.buffer == null) {
                    if (_dns_forest_buffers < 0 || _dns_forest_buffers > 65535) {
                        throw new NdrException(NdrException.INVALID_CONFORMANCE);
                    }
                    this.dns_forest.buffer = new short[_dns_forest_buffers];
                }
                _src = _src4.derive(_dns_forest_bufferi);
                for (int _i3 = 0; _i3 < _dns_forest_bufferl; _i3++) {
                    this.dns_forest.buffer[_i3] = (short) _src.dec_ndr_short();
                }
            }
            if (this.domain_guid.node == null) {
                if (6 < 0 || 6 > 65535) {
                    throw new NdrException(NdrException.INVALID_CONFORMANCE);
                }
                this.domain_guid.node = new byte[6];
            }
            NdrBuffer _src5 = _src.derive(_domain_guid_nodei);
            for (int _i4 = 0; _i4 < 6; _i4++) {
                this.domain_guid.node[_i4] = (byte) _src5.dec_ndr_small();
            }
            if (_sidp != 0) {
                if (this.sid == null) {
                    this.sid = new rpc.sid_t();
                }
                this.sid.decode(_src5.deferred);
            }
        }
    }

    public static class LsarSidPtr extends NdrObject {
        public rpc.sid_t sid;

        @Override // jcifs.dcerpc.ndr.NdrObject
        public void encode(NdrBuffer _dst) throws NdrException {
            _dst.align(4);
            _dst.enc_ndr_referent(this.sid, 1);
            if (this.sid != null) {
                this.sid.encode(_dst.deferred);
            }
        }

        @Override // jcifs.dcerpc.ndr.NdrObject
        public void decode(NdrBuffer _src) throws NdrException {
            _src.align(4);
            if (_src.dec_ndr_long() != 0) {
                if (this.sid == null) {
                    this.sid = new rpc.sid_t();
                }
                this.sid.decode(_src.deferred);
            }
        }
    }

    public static class LsarSidArray extends NdrObject {
        public int num_sids;
        public LsarSidPtr[] sids;

        @Override // jcifs.dcerpc.ndr.NdrObject
        public void encode(NdrBuffer _dst) throws NdrException {
            _dst.align(4);
            _dst.enc_ndr_long(this.num_sids);
            _dst.enc_ndr_referent(this.sids, 1);
            if (this.sids != null) {
                NdrBuffer _dst2 = _dst.deferred;
                int _sidss = this.num_sids;
                _dst2.enc_ndr_long(_sidss);
                int _sidsi = _dst2.index;
                _dst2.advance(_sidss * 4);
                NdrBuffer _dst3 = _dst2.derive(_sidsi);
                for (int _i = 0; _i < _sidss; _i++) {
                    this.sids[_i].encode(_dst3);
                }
            }
        }

        @Override // jcifs.dcerpc.ndr.NdrObject
        public void decode(NdrBuffer _src) throws NdrException {
            _src.align(4);
            this.num_sids = _src.dec_ndr_long();
            if (_src.dec_ndr_long() != 0) {
                NdrBuffer _src2 = _src.deferred;
                int _sidss = _src2.dec_ndr_long();
                int _sidsi = _src2.index;
                _src2.advance(_sidss * 4);
                if (this.sids == null) {
                    if (_sidss < 0 || _sidss > 65535) {
                        throw new NdrException(NdrException.INVALID_CONFORMANCE);
                    }
                    this.sids = new LsarSidPtr[_sidss];
                }
                NdrBuffer _src3 = _src2.derive(_sidsi);
                for (int _i = 0; _i < _sidss; _i++) {
                    if (this.sids[_i] == null) {
                        this.sids[_i] = new LsarSidPtr();
                    }
                    this.sids[_i].decode(_src3);
                }
            }
        }
    }

    public static class LsarTranslatedSid extends NdrObject {
        public int rid;
        public int sid_index;
        public int sid_type;

        @Override // jcifs.dcerpc.ndr.NdrObject
        public void encode(NdrBuffer _dst) throws NdrException {
            _dst.align(4);
            _dst.enc_ndr_short(this.sid_type);
            _dst.enc_ndr_long(this.rid);
            _dst.enc_ndr_long(this.sid_index);
        }

        @Override // jcifs.dcerpc.ndr.NdrObject
        public void decode(NdrBuffer _src) throws NdrException {
            _src.align(4);
            this.sid_type = _src.dec_ndr_short();
            this.rid = _src.dec_ndr_long();
            this.sid_index = _src.dec_ndr_long();
        }
    }

    public static class LsarTransSidArray extends NdrObject {
        public int count;
        public LsarTranslatedSid[] sids;

        @Override // jcifs.dcerpc.ndr.NdrObject
        public void encode(NdrBuffer _dst) throws NdrException {
            _dst.align(4);
            _dst.enc_ndr_long(this.count);
            _dst.enc_ndr_referent(this.sids, 1);
            if (this.sids != null) {
                NdrBuffer _dst2 = _dst.deferred;
                int _sidss = this.count;
                _dst2.enc_ndr_long(_sidss);
                int _sidsi = _dst2.index;
                _dst2.advance(_sidss * 12);
                NdrBuffer _dst3 = _dst2.derive(_sidsi);
                for (int _i = 0; _i < _sidss; _i++) {
                    this.sids[_i].encode(_dst3);
                }
            }
        }

        @Override // jcifs.dcerpc.ndr.NdrObject
        public void decode(NdrBuffer _src) throws NdrException {
            _src.align(4);
            this.count = _src.dec_ndr_long();
            if (_src.dec_ndr_long() != 0) {
                NdrBuffer _src2 = _src.deferred;
                int _sidss = _src2.dec_ndr_long();
                int _sidsi = _src2.index;
                _src2.advance(_sidss * 12);
                if (this.sids == null) {
                    if (_sidss < 0 || _sidss > 65535) {
                        throw new NdrException(NdrException.INVALID_CONFORMANCE);
                    }
                    this.sids = new LsarTranslatedSid[_sidss];
                }
                NdrBuffer _src3 = _src2.derive(_sidsi);
                for (int _i = 0; _i < _sidss; _i++) {
                    if (this.sids[_i] == null) {
                        this.sids[_i] = new LsarTranslatedSid();
                    }
                    this.sids[_i].decode(_src3);
                }
            }
        }
    }

    public static class LsarTrustInformation extends NdrObject {
        public rpc.unicode_string name;
        public rpc.sid_t sid;

        @Override // jcifs.dcerpc.ndr.NdrObject
        public void encode(NdrBuffer _dst) throws NdrException {
            _dst.align(4);
            _dst.enc_ndr_short(this.name.length);
            _dst.enc_ndr_short(this.name.maximum_length);
            _dst.enc_ndr_referent(this.name.buffer, 1);
            _dst.enc_ndr_referent(this.sid, 1);
            if (this.name.buffer != null) {
                NdrBuffer _dst2 = _dst.deferred;
                int _name_bufferl = this.name.length / 2;
                _dst2.enc_ndr_long(this.name.maximum_length / 2);
                _dst2.enc_ndr_long(0);
                _dst2.enc_ndr_long(_name_bufferl);
                int _name_bufferi = _dst2.index;
                _dst2.advance(_name_bufferl * 2);
                _dst = _dst2.derive(_name_bufferi);
                for (int _i = 0; _i < _name_bufferl; _i++) {
                    _dst.enc_ndr_short(this.name.buffer[_i]);
                }
            }
            if (this.sid != null) {
                this.sid.encode(_dst.deferred);
            }
        }

        @Override // jcifs.dcerpc.ndr.NdrObject
        public void decode(NdrBuffer _src) throws NdrException {
            _src.align(4);
            _src.align(4);
            if (this.name == null) {
                this.name = new rpc.unicode_string();
            }
            this.name.length = (short) _src.dec_ndr_short();
            this.name.maximum_length = (short) _src.dec_ndr_short();
            int _name_bufferp = _src.dec_ndr_long();
            int _sidp = _src.dec_ndr_long();
            if (_name_bufferp != 0) {
                NdrBuffer _src2 = _src.deferred;
                int _name_buffers = _src2.dec_ndr_long();
                _src2.dec_ndr_long();
                int _name_bufferl = _src2.dec_ndr_long();
                int _name_bufferi = _src2.index;
                _src2.advance(_name_bufferl * 2);
                if (this.name.buffer == null) {
                    if (_name_buffers < 0 || _name_buffers > 65535) {
                        throw new NdrException(NdrException.INVALID_CONFORMANCE);
                    }
                    this.name.buffer = new short[_name_buffers];
                }
                _src = _src2.derive(_name_bufferi);
                for (int _i = 0; _i < _name_bufferl; _i++) {
                    this.name.buffer[_i] = (short) _src.dec_ndr_short();
                }
            }
            if (_sidp != 0) {
                if (this.sid == null) {
                    this.sid = new rpc.sid_t();
                }
                this.sid.decode(_src.deferred);
            }
        }
    }

    public static class LsarRefDomainList extends NdrObject {
        public int count;
        public LsarTrustInformation[] domains;
        public int max_count;

        @Override // jcifs.dcerpc.ndr.NdrObject
        public void encode(NdrBuffer _dst) throws NdrException {
            _dst.align(4);
            _dst.enc_ndr_long(this.count);
            _dst.enc_ndr_referent(this.domains, 1);
            _dst.enc_ndr_long(this.max_count);
            if (this.domains != null) {
                NdrBuffer _dst2 = _dst.deferred;
                int _domainss = this.count;
                _dst2.enc_ndr_long(_domainss);
                int _domainsi = _dst2.index;
                _dst2.advance(_domainss * 12);
                NdrBuffer _dst3 = _dst2.derive(_domainsi);
                for (int _i = 0; _i < _domainss; _i++) {
                    this.domains[_i].encode(_dst3);
                }
            }
        }

        @Override // jcifs.dcerpc.ndr.NdrObject
        public void decode(NdrBuffer _src) throws NdrException {
            _src.align(4);
            this.count = _src.dec_ndr_long();
            int _domainsp = _src.dec_ndr_long();
            this.max_count = _src.dec_ndr_long();
            if (_domainsp != 0) {
                NdrBuffer _src2 = _src.deferred;
                int _domainss = _src2.dec_ndr_long();
                int _domainsi = _src2.index;
                _src2.advance(_domainss * 12);
                if (this.domains == null) {
                    if (_domainss < 0 || _domainss > 65535) {
                        throw new NdrException(NdrException.INVALID_CONFORMANCE);
                    }
                    this.domains = new LsarTrustInformation[_domainss];
                }
                NdrBuffer _src3 = _src2.derive(_domainsi);
                for (int _i = 0; _i < _domainss; _i++) {
                    if (this.domains[_i] == null) {
                        this.domains[_i] = new LsarTrustInformation();
                    }
                    this.domains[_i].decode(_src3);
                }
            }
        }
    }

    public static class LsarTranslatedName extends NdrObject {
        public rpc.unicode_string name;
        public int sid_index;
        public short sid_type;

        @Override // jcifs.dcerpc.ndr.NdrObject
        public void encode(NdrBuffer _dst) throws NdrException {
            _dst.align(4);
            _dst.enc_ndr_short(this.sid_type);
            _dst.enc_ndr_short(this.name.length);
            _dst.enc_ndr_short(this.name.maximum_length);
            _dst.enc_ndr_referent(this.name.buffer, 1);
            _dst.enc_ndr_long(this.sid_index);
            if (this.name.buffer != null) {
                NdrBuffer _dst2 = _dst.deferred;
                int _name_bufferl = this.name.length / 2;
                _dst2.enc_ndr_long(this.name.maximum_length / 2);
                _dst2.enc_ndr_long(0);
                _dst2.enc_ndr_long(_name_bufferl);
                int _name_bufferi = _dst2.index;
                _dst2.advance(_name_bufferl * 2);
                NdrBuffer _dst3 = _dst2.derive(_name_bufferi);
                for (int _i = 0; _i < _name_bufferl; _i++) {
                    _dst3.enc_ndr_short(this.name.buffer[_i]);
                }
            }
        }

        @Override // jcifs.dcerpc.ndr.NdrObject
        public void decode(NdrBuffer _src) throws NdrException {
            _src.align(4);
            this.sid_type = (short) _src.dec_ndr_short();
            _src.align(4);
            if (this.name == null) {
                this.name = new rpc.unicode_string();
            }
            this.name.length = (short) _src.dec_ndr_short();
            this.name.maximum_length = (short) _src.dec_ndr_short();
            int _name_bufferp = _src.dec_ndr_long();
            this.sid_index = _src.dec_ndr_long();
            if (_name_bufferp != 0) {
                NdrBuffer _src2 = _src.deferred;
                int _name_buffers = _src2.dec_ndr_long();
                _src2.dec_ndr_long();
                int _name_bufferl = _src2.dec_ndr_long();
                int _name_bufferi = _src2.index;
                _src2.advance(_name_bufferl * 2);
                if (this.name.buffer == null) {
                    if (_name_buffers < 0 || _name_buffers > 65535) {
                        throw new NdrException(NdrException.INVALID_CONFORMANCE);
                    }
                    this.name.buffer = new short[_name_buffers];
                }
                NdrBuffer _src3 = _src2.derive(_name_bufferi);
                for (int _i = 0; _i < _name_bufferl; _i++) {
                    this.name.buffer[_i] = (short) _src3.dec_ndr_short();
                }
            }
        }
    }

    public static class LsarTransNameArray extends NdrObject {
        public int count;
        public LsarTranslatedName[] names;

        @Override // jcifs.dcerpc.ndr.NdrObject
        public void encode(NdrBuffer _dst) throws NdrException {
            _dst.align(4);
            _dst.enc_ndr_long(this.count);
            _dst.enc_ndr_referent(this.names, 1);
            if (this.names != null) {
                NdrBuffer _dst2 = _dst.deferred;
                int _namess = this.count;
                _dst2.enc_ndr_long(_namess);
                int _namesi = _dst2.index;
                _dst2.advance(_namess * 16);
                NdrBuffer _dst3 = _dst2.derive(_namesi);
                for (int _i = 0; _i < _namess; _i++) {
                    this.names[_i].encode(_dst3);
                }
            }
        }

        @Override // jcifs.dcerpc.ndr.NdrObject
        public void decode(NdrBuffer _src) throws NdrException {
            _src.align(4);
            this.count = _src.dec_ndr_long();
            if (_src.dec_ndr_long() != 0) {
                NdrBuffer _src2 = _src.deferred;
                int _namess = _src2.dec_ndr_long();
                int _namesi = _src2.index;
                _src2.advance(_namess * 16);
                if (this.names == null) {
                    if (_namess < 0 || _namess > 65535) {
                        throw new NdrException(NdrException.INVALID_CONFORMANCE);
                    }
                    this.names = new LsarTranslatedName[_namess];
                }
                NdrBuffer _src3 = _src2.derive(_namesi);
                for (int _i = 0; _i < _namess; _i++) {
                    if (this.names[_i] == null) {
                        this.names[_i] = new LsarTranslatedName();
                    }
                    this.names[_i].decode(_src3);
                }
            }
        }
    }

    public static class LsarClose extends DcerpcMessage {
        public rpc.policy_handle handle;
        public int retval;

        @Override // jcifs.dcerpc.DcerpcMessage
        public int getOpnum() {
            return 0;
        }

        public LsarClose(rpc.policy_handle handle2) {
            this.handle = handle2;
        }

        @Override // jcifs.dcerpc.DcerpcMessage
        public void encode_in(NdrBuffer _dst) throws NdrException {
            this.handle.encode(_dst);
        }

        @Override // jcifs.dcerpc.DcerpcMessage
        public void decode_out(NdrBuffer _src) throws NdrException {
            this.handle.decode(_src);
            this.retval = _src.dec_ndr_long();
        }
    }

    public static class LsarQueryInformationPolicy extends DcerpcMessage {
        public rpc.policy_handle handle;
        public NdrObject info;
        public short level;
        public int retval;

        @Override // jcifs.dcerpc.DcerpcMessage
        public int getOpnum() {
            return 7;
        }

        public LsarQueryInformationPolicy(rpc.policy_handle handle2, short level2, NdrObject info2) {
            this.handle = handle2;
            this.level = level2;
            this.info = info2;
        }

        @Override // jcifs.dcerpc.DcerpcMessage
        public void encode_in(NdrBuffer _dst) throws NdrException {
            this.handle.encode(_dst);
            _dst.enc_ndr_short(this.level);
        }

        @Override // jcifs.dcerpc.DcerpcMessage
        public void decode_out(NdrBuffer _src) throws NdrException {
            if (_src.dec_ndr_long() != 0) {
                _src.dec_ndr_short();
                this.info.decode(_src);
            }
            this.retval = _src.dec_ndr_long();
        }
    }

    public static class LsarLookupSids extends DcerpcMessage {
        public int count;
        public LsarRefDomainList domains;
        public rpc.policy_handle handle;
        public short level;
        public LsarTransNameArray names;
        public int retval;
        public LsarSidArray sids;

        @Override // jcifs.dcerpc.DcerpcMessage
        public int getOpnum() {
            return 15;
        }

        public LsarLookupSids(rpc.policy_handle handle2, LsarSidArray sids2, LsarRefDomainList domains2, LsarTransNameArray names2, short level2, int count2) {
            this.handle = handle2;
            this.sids = sids2;
            this.domains = domains2;
            this.names = names2;
            this.level = level2;
            this.count = count2;
        }

        @Override // jcifs.dcerpc.DcerpcMessage
        public void encode_in(NdrBuffer _dst) throws NdrException {
            this.handle.encode(_dst);
            this.sids.encode(_dst);
            this.names.encode(_dst);
            _dst.enc_ndr_short(this.level);
            _dst.enc_ndr_long(this.count);
        }

        @Override // jcifs.dcerpc.DcerpcMessage
        public void decode_out(NdrBuffer _src) throws NdrException {
            if (_src.dec_ndr_long() != 0) {
                if (this.domains == null) {
                    this.domains = new LsarRefDomainList();
                }
                this.domains.decode(_src);
            }
            this.names.decode(_src);
            this.count = _src.dec_ndr_long();
            this.retval = _src.dec_ndr_long();
        }
    }

    public static class LsarOpenPolicy2 extends DcerpcMessage {
        public int desired_access;
        public LsarObjectAttributes object_attributes;
        public rpc.policy_handle policy_handle;
        public int retval;
        public String system_name;

        @Override // jcifs.dcerpc.DcerpcMessage
        public int getOpnum() {
            return 44;
        }

        public LsarOpenPolicy2(String system_name2, LsarObjectAttributes object_attributes2, int desired_access2, rpc.policy_handle policy_handle2) {
            this.system_name = system_name2;
            this.object_attributes = object_attributes2;
            this.desired_access = desired_access2;
            this.policy_handle = policy_handle2;
        }

        @Override // jcifs.dcerpc.DcerpcMessage
        public void encode_in(NdrBuffer _dst) throws NdrException {
            _dst.enc_ndr_referent(this.system_name, 1);
            if (this.system_name != null) {
                _dst.enc_ndr_string(this.system_name);
            }
            this.object_attributes.encode(_dst);
            _dst.enc_ndr_long(this.desired_access);
        }

        @Override // jcifs.dcerpc.DcerpcMessage
        public void decode_out(NdrBuffer _src) throws NdrException {
            this.policy_handle.decode(_src);
            this.retval = _src.dec_ndr_long();
        }
    }

    public static class LsarQueryInformationPolicy2 extends DcerpcMessage {
        public rpc.policy_handle handle;
        public NdrObject info;
        public short level;
        public int retval;

        @Override // jcifs.dcerpc.DcerpcMessage
        public int getOpnum() {
            return 46;
        }

        public LsarQueryInformationPolicy2(rpc.policy_handle handle2, short level2, NdrObject info2) {
            this.handle = handle2;
            this.level = level2;
            this.info = info2;
        }

        @Override // jcifs.dcerpc.DcerpcMessage
        public void encode_in(NdrBuffer _dst) throws NdrException {
            this.handle.encode(_dst);
            _dst.enc_ndr_short(this.level);
        }

        @Override // jcifs.dcerpc.DcerpcMessage
        public void decode_out(NdrBuffer _src) throws NdrException {
            if (_src.dec_ndr_long() != 0) {
                _src.dec_ndr_short();
                this.info.decode(_src);
            }
            this.retval = _src.dec_ndr_long();
        }
    }
}
