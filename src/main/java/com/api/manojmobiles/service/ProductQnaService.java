package com.api.manojmobiles.service;

import com.api.manojmobiles.dto.qna.AnswerResponseDTO;
import com.api.manojmobiles.dto.qna.CreateAnswerRequestDTO;
import com.api.manojmobiles.dto.qna.CreateQuestionRequestDTO;
import com.api.manojmobiles.dto.qna.QuestionResponseDTO;
import com.api.manojmobiles.entity.Product;
import com.api.manojmobiles.entity.ProductAnswer;
import com.api.manojmobiles.entity.ProductQuestion;
import com.api.manojmobiles.entity.User;
import com.api.manojmobiles.entity.enums.Role;
import com.api.manojmobiles.exception.ResourceNotFoundException;
import com.api.manojmobiles.repository.ProductAnswerRepository;
import com.api.manojmobiles.repository.ProductQuestionRepository;
import com.api.manojmobiles.repository.ProductRepository;
import com.api.manojmobiles.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductQnaService {

    private final ProductQuestionRepository questionRepository;
    private final ProductAnswerRepository answerRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public QuestionResponseDTO askQuestion(String username, CreateQuestionRequestDTO dto) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        ProductQuestion question = ProductQuestion.builder()
                .product(product)
                .user(user)
                .questionText(dto.getQuestionText())
                .isApproved(true)
                .build();

        question = questionRepository.save(question);
        return mapToQuestionDTO(question);
    }

    @Transactional
    public AnswerResponseDTO answerQuestion(String username, UUID questionId, CreateAnswerRequestDTO dto) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ProductQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));

        boolean isSeller = user.getRole() == Role.ADMIN; // Or check specific permission

        ProductAnswer answer = ProductAnswer.builder()
                .question(question)
                .user(user)
                .answerText(dto.getAnswerText())
                .isSellerAnswer(isSeller)
                .build();

        answer = answerRepository.save(answer);
        return mapToAnswerDTO(answer);
    }

    public Page<QuestionResponseDTO> getApprovedQuestionsForProduct(UUID productId, Pageable pageable) {
        return questionRepository.findByProductIdAndIsApprovedTrue(productId, pageable)
                .map(this::mapToQuestionDTO);
    }

    @Transactional
    public void adminDeleteQuestion(UUID questionId) {
        ProductQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));
        // Due to cascade = CascadeType.ALL, deleting the question deletes all its answers.
        questionRepository.delete(question);
    }

    private QuestionResponseDTO mapToQuestionDTO(ProductQuestion question) {
        List<ProductAnswer> answers = answerRepository.findByQuestionId(question.getId());
        List<AnswerResponseDTO> answerDTOs = answers.stream()
                .map(this::mapToAnswerDTO)
                .collect(Collectors.toList());

        return QuestionResponseDTO.builder()
                .id(question.getId())
                .productId(question.getProduct().getId())
                .productName(question.getProduct().getName())
                .userId(question.getUser().getId())
                .userName(question.getUser().getName())
                .questionText(question.getQuestionText())
                .createdAt(question.getCreatedAt())
                .answers(answerDTOs)
                .build();
    }

    private AnswerResponseDTO mapToAnswerDTO(ProductAnswer answer) {
        return AnswerResponseDTO.builder()
                .id(answer.getId())
                .userId(answer.getUser().getId())
                .userName(answer.getUser().getName())
                .answerText(answer.getAnswerText())
                .isSellerAnswer(answer.getIsSellerAnswer())
                .createdAt(answer.getCreatedAt())
                .build();
    }
}
