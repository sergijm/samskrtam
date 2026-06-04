package sm.selflearn.samskrtam.user.repository;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import sm.selflearn.samskrtam.user.model.UserProfile;
import sm.selflearn.samskrtam.user.model.UserRole;


import java.util.ArrayList;
import java.util.List;

public class UserProfileSpecification {

    public static Specification<UserProfile> filterBy(String search, UserRole role, Boolean blocked) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String lowerCaseSearch = "%" + search.toLowerCase() + "%";
                Predicate searchPredicate = criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), lowerCaseSearch),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), lowerCaseSearch),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), lowerCaseSearch),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), lowerCaseSearch)
                );
                predicates.add(searchPredicate);
            }

            if (role != null) {
                // To filter by a role within a Set<UserRole>
                predicates.add(criteriaBuilder.isMember(role, root.get("roles")));
            }

            if (blocked != null) {
                predicates.add(criteriaBuilder.equal(root.get("blocked"), blocked));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
