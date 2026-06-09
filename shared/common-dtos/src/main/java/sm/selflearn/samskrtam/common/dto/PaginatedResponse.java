package sm.selflearn.samskrtam.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaginatedResponse<T> {
    private List<T> content;
    private int totalPages;
    private long totalElements;
    private int currentPage;
    private int pageSize;
    private boolean isFirst;
    private boolean isLast;
}
