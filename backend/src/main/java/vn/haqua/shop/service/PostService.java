package vn.haqua.shop.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.haqua.shop.dto.PageDto;
import vn.haqua.shop.dto.PostDtos.PostDto;
import vn.haqua.shop.dto.PostDtos.PostRequest;
import vn.haqua.shop.entity.Post;
import vn.haqua.shop.exception.ApiException;
import vn.haqua.shop.repository.PostRepository;
import vn.haqua.shop.util.SlugUtil;

/** UC21 – Cẩm nang công nghệ. */
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository posts;

    @Transactional(readOnly = true)
    public PageDto<PostDto> published(int page, int size) {
        return PageDto.of(posts.findByPublishedAtNotNull(
                PageRequest.of(page, size, Sort.by("publishedAt").descending())), PostService::toDto);
    }

    @Transactional(readOnly = true)
    public PostDto bySlug(String slug) {
        return toDto(posts.findBySlugAndPublishedAtNotNull(slug)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy bài viết")));
    }

    @Transactional(readOnly = true)
    public PageDto<PostDto> adminList(int page, int size) {
        return PageDto.of(posts.findAll(PageRequest.of(page, size, Sort.by("id").descending())), PostService::toDto);
    }

    @Transactional
    public PostDto save(Long id, Long authorId, PostRequest req) {
        Post p = id == null ? new Post() : posts.findById(id).orElseThrow(() -> ApiException.notFound("Không thấy bài viết"));
        String slug = req.slug() == null || req.slug().isBlank() ? SlugUtil.toSlug(req.title()) : SlugUtil.toSlug(req.slug());
        boolean taken = id == null ? posts.existsBySlug(slug) : posts.existsBySlugAndIdNot(slug, id);
        if (taken) {
            throw ApiException.conflict("Slug đã tồn tại");
        }
        if (id == null) {
            p.setAuthorId(authorId);
        }
        p.setTitle(req.title());
        p.setSlug(slug);
        p.setTag(req.tag());
        p.setContent(req.content());
        p.setCoverUrl(req.coverUrl());
        p.setMetaTitle(req.metaTitle());
        p.setMetaDescription(req.metaDescription());
        if (req.publish() && p.getPublishedAt() == null) {
            p.setPublishedAt(java.time.Instant.now());
        } else if (!req.publish()) {
            p.setPublishedAt(null);
        }
        return toDto(posts.save(p));
    }

    private static PostDto toDto(Post p) {
        return new PostDto(p.getId(), p.getTitle(), p.getSlug(), p.getTag(), p.getContent(), p.getCoverUrl(),
                p.getMetaTitle(), p.getMetaDescription(), p.getPublishedAt());

    }
}
