package com.studioos.common;
import java.util.List;
import org.springframework.data.domain.Page;
public record PageResult<T>(List<T> items,int page,int size,long totalElements,int totalPages) {
    public static <T> PageResult<T> from(Page<T> page) {
        return new PageResult<>(page.getContent(),page.getNumber(),page.getSize(),page.getTotalElements(),page.getTotalPages());
    }
    public static void validate(int page,int size) {
        if(page<0 || size<1 || size>100) throw new ApiException(400,"INVALID_PAGE","페이지 크기는 1~100이어야 합니다.");
    }
}
