package com.beratcan.app.service;

import com.beratcan.app.dto.request.PublisherRequest;
import com.beratcan.app.dto.response.PublisherResponse;
import com.beratcan.app.entity.Publisher;
import com.beratcan.app.exception.BadRequestException;
import com.beratcan.app.exception.PublisherNotFoundException;
import com.beratcan.app.repository.BookRepository;
import com.beratcan.app.repository.PublisherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PublisherService {

    private final PublisherRepository publisherRepository;
    private final BookRepository bookRepository;

    @Transactional
    public PublisherResponse createPublisher(PublisherRequest request) {
        validateRequest(request);
        if (publisherRepository.existsByNameIgnoreCase(request.getName().trim())) {
            throw new BadRequestException("Bu yayınevi adı zaten kullanılıyor");
        }
        Publisher publisher = new Publisher();
        publisher.setName(request.getName().trim());
        publisher.setDescription(request.getDescription());
        Publisher saved = publisherRepository.save(publisher);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<PublisherResponse> getPublishers() {
        return publisherRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PublisherResponse getPublisher(Long id) {
        Publisher publisher = publisherRepository.findById(id)
                .orElseThrow(() -> new PublisherNotFoundException(id));
        return toResponse(publisher);
    }

    @Transactional
    public PublisherResponse updatePublisher(Long id, PublisherRequest request) {
        validateRequest(request);
        Publisher publisher = publisherRepository.findById(id)
                .orElseThrow(() -> new PublisherNotFoundException(id));

        String normalizedName = request.getName().trim();
        if (!publisher.getName().equalsIgnoreCase(normalizedName)
                && publisherRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new BadRequestException("Bu yayınevi adı zaten kullanılıyor");
        }

        publisher.setName(normalizedName);
        publisher.setDescription(request.getDescription());
        Publisher saved = publisherRepository.save(publisher);
        return toResponse(saved);
    }

    @Transactional
    public void deletePublisher(Long id) {
        Publisher publisher = publisherRepository.findById(id)
                .orElseThrow(() -> new PublisherNotFoundException(id));
        if (bookRepository.existsByPublisher_Id(id)) {
            throw new BadRequestException("Bu yayınevine ait kitaplar bulunduğu için silinemez");
        }
        publisherRepository.delete(publisher);
    }

    private void validateRequest(PublisherRequest request) {
        if (request == null || !StringUtils.hasText(request.getName())) {
            throw new BadRequestException("Yayınevi adı gereklidir");
        }
    }

    private PublisherResponse toResponse(Publisher publisher) {
        PublisherResponse response = new PublisherResponse();
        response.setId(publisher.getId());
        response.setName(publisher.getName());
        response.setDescription(publisher.getDescription());
        long bookCount = publisher.getId() == null ? 0 : bookRepository.countByPublisher_Id(publisher.getId());
        response.setBookCount(bookCount);
        return response;
    }
}
