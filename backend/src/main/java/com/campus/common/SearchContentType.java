package com.campus.common;

public enum SearchContentType {

    ALL("all", "全部"),
    POST("post", "社区帖子"),
    JOB("job", "岗位"),
    RESOURCE("resource", "资料"),
    RESUME("resume", "简历"),
    NOTIFICATION("notification", "通知"),
    APPLICATION("application", "投递记录");

    private final String code;
    private final String label;

    SearchContentType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static SearchContentType fromCode(String code) {
        for (SearchContentType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return ALL;
    }

    public boolean isAll() {
        return this == ALL;
    }

    public boolean isPublic() {
        return this == POST || this == JOB || this == RESOURCE;
    }

    public boolean isPrivate() {
        return this == RESUME || this == NOTIFICATION || this == APPLICATION;
    }
}
