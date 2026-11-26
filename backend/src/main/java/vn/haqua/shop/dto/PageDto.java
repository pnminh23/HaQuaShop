package vn.haqua.shop.dto;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

public record PageDto<T>(List<T> content, long totalElements, int totalPages, int page, int size) {

    public static <E, T> PageDto<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageDto<>(page.getContent().stream().map(mapper).toList(), page.getTotalElements(),
                page.getTotalPages(), page.getNumber(), page.getSize());

    }
}
