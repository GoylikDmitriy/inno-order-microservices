package com.goylik.payment_service.repository.impl;

import com.goylik.payment_service.model.enums.PaymentStatus;
import com.goylik.payment_service.repository.PaymentStatisticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class PaymentStatisticsRepositoryImpl implements PaymentStatisticsRepository {
    private final MongoTemplate mongoTemplate;

    @Override
    public BigDecimal getTotalAmountByUserIdAndDateRange(Long userId, LocalDateTime from, LocalDateTime to) {
        var matchStage = Aggregation.match(
                Criteria.where("user_id").is(userId)
                        .and("timestamp").gte(from).lte(to)
                        .and("status").is(PaymentStatus.SUCCESS.name())
        );

        return executeAggregation(matchStage);
    }

    @Override
    public BigDecimal getTotalAmountByDateRange(LocalDateTime from, LocalDateTime to) {
        var matchStage = Aggregation.match(
                Criteria.where("timestamp").gte(from).lte(to)
                        .and("status").is(PaymentStatus.SUCCESS.name())
        );

        return executeAggregation(matchStage);
    }

    private BigDecimal executeAggregation(MatchOperation matchStage) {
        var groupStage = Aggregation.group()
                .sum("payment_amount").as("total");

        var aggregation = Aggregation.newAggregation(matchStage, groupStage);

        var result = mongoTemplate.aggregate(
                aggregation,
                "payments",
                TotalAmountResult.class
        );

        return result.getUniqueMappedResult() != null
                ? result.getUniqueMappedResult().total()
                : BigDecimal.ZERO;
    }

    private record TotalAmountResult(BigDecimal total) {}
}
