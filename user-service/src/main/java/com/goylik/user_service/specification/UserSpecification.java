package com.goylik.user_service.specification;

import com.goylik.user_service.model.entity.User;
import org.springframework.data.jpa.domain.Specification;

/**
 * Provides specifications for filtering {@link User} entities.
 * <p>
 * This class contains static factory methods for creating JPA specifications
 * to query users based on various criteria like first name and surname.
 * </p>
 */
public final class UserSpecification {
    private UserSpecification() {}

    /**
     * Creates a specification to filter users by first name (case-insensitive, partial match).
     *
     * @param name the first name to search for (can be null)
     * @return a specification for first name filtering, or {@code conjunction()} if the input is null
     */
    public static Specification<User> hasFirstNameLike(String name) {
        return (root, query, cb) ->
                (name == null || name.isBlank()) ? cb.conjunction() :
                        cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    /**
     * Creates a specification to filter users by surname (case-insensitive, partial match).
     *
     * @param surname the surname to search for (can be null)
     * @return a specification for surname filtering, or {@code conjunction()} if the input is null
     */
    public static Specification<User> hasSurnameLike(String surname) {
        return (root, query, cb) ->
                (surname == null || surname.isBlank()) ? cb.conjunction() :
                        cb.like(cb.lower(root.get("surname")), "%" + surname.toLowerCase() + "%");
    }

    /**
     * Combines first name and surname specifications using logical AND.
     *
     * @param name    the first name to search for (can be null)
     * @param surname the surname to search for (can be null)
     * @return a combined specification that matches both criteria
     */
    public static Specification<User> hasFirstNameLikeAndHasSurnameLike(
            String name,
            String surname) {
        return Specification
                .where(hasFirstNameLike(name))
                .and(hasSurnameLike(surname));
    }
}