package vn.haqua.shop.controller;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import vn.haqua.shop.dto.PageDto;
import vn.haqua.shop.dto.PostDtos.PostDto;
import vn.haqua.shop.dto.PostDtos.PostRequest;
import vn.haqua.shop.security.CurrentUser;
import vn.haqua.shop.service.PostService;

@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService service;

    @GetMapping("/api/posts")
    PageDto<PostDto> list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "9") int size) {
        return service.published(page, Math.min(size, 30));
    }

    @GetMapping("/api/posts/{slug}")
    PostDto get(@PathVariable String slug) {
        return service.bySlug(slug);
    }

    @GetMapping("/api/admin/posts")
    @PreAuthorize("hasRole('ADMIN')")
    PageDto<PostDto> adminList(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.adminList(page, size);
    }

    @PostMapping("/api/admin/posts")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    PostDto create(@Valid @RequestBody PostRequest req) {
        return service.save(null, CurrentUser.requireId(), req);
    }

    @PutMapping("/api/admin/posts/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    PostDto update(@PathVariable Long id, @Valid @RequestBody PostRequest req) {
        return service.save(id, CurrentUser.requireId(), req);

    }
}
