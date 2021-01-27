package kitchenpos.application;

import kitchenpos.menu.dto.MenuProductRequest;
import kitchenpos.menu.dto.MenuRequest;
import kitchenpos.menu.dto.MenuResponse;
import kitchenpos.menugroup.dto.MenuGroupRequest;
import kitchenpos.menugroup.dto.MenuGroupResponse;
import kitchenpos.order.dto.OrderMenuRequest;
import kitchenpos.order.dto.OrderRequest;
import kitchenpos.product.dto.ProductRequest;
import kitchenpos.product.dto.ProductResponse;
import kitchenpos.ordertable.domain.OrderTable;
import kitchenpos.menu.application.MenuService;
import kitchenpos.menugroup.application.MenuGroupService;
import kitchenpos.order.application.OrderService;
import kitchenpos.product.application.ProductService;
import kitchenpos.ordertable.application.OrderTableService;
import kitchenpos.ordertable.dto.OrderTableRequest;
import kitchenpos.ordertable.dto.OrderTableResponse;
import kitchenpos.tablegroup.application.TableGroupService;
import kitchenpos.tablegroup.dto.TableGroupRequest;
import kitchenpos.tablegroup.dto.TableGroupResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("단체 지정 서비스에 관련한 기능")
@SpringBootTest
class TableGroupServiceTest {
    @Autowired
    private OrderService orderService;
    @Autowired
    private TableGroupService tableGroupService;
    @Autowired
    private OrderTableService orderTableService;
    @Autowired
    private ProductService productService;
    @Autowired
    private MenuGroupService menuGroupService;
    @Autowired
    private MenuService menuService;

    @DisplayName("`단체 지정`을 생성한다.")
    @Test
    void createTableGroup() {
        // Given
        OrderTableResponse orderTable1 = orderTableService.create(new OrderTableRequest(0, true));
        OrderTableResponse orderTable2 = orderTableService.create(new OrderTableRequest(0, true));
        List<OrderTableRequest> orderTableRequests = Arrays.asList(new OrderTableRequest(orderTable1.getId()),
                new OrderTableRequest(orderTable2.getId()));
        TableGroupRequest request = new TableGroupRequest(orderTableRequests);

        // When
        TableGroupResponse response = tableGroupService.create(request);
        OrderTableResponse foundOrderTable1 = orderTableService.findResponseById(orderTable1.getId());
        OrderTableResponse foundOrderTable2 = orderTableService.findResponseById(orderTable2.getId());

        // Then
        assertAll(
                () -> assertThat(response).isNotNull(),
                () -> assertThat(response.getId()).isNotNull(),
                () -> assertThat(response.getOrderTables()).containsAnyElementsOf(Arrays.asList(foundOrderTable1, foundOrderTable2)),
                () -> assertThat(response.getCreatedAt()).isNotNull()
        );
    }

    @DisplayName("`단체 지정`으로 등록할 `주문 테이블`이 2개 미만이면 생성할 수 없다.")
    @Test
    void exceptionToCreateTableGroupWithZeroOrOneOrderTable() {
        // Given
        OrderTableResponse orderTable1 = orderTableService.create(new OrderTableRequest(0, true));
        List<OrderTableRequest> orderTableRequests = Collections.singletonList(new OrderTableRequest(orderTable1.getId()));
        TableGroupRequest request = new TableGroupRequest(orderTableRequests);

        // When & Then
        assertThatThrownBy(() -> tableGroupService.create(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("`단체 지정`할 `주문 테이블`은 비어있어야한다.")
    @Test
    void exceptionToCreateTableGroupWithNonemptyOrderTable() {
        // Given
        OrderTableResponse invalidOrderTable1 = orderTableService.create(new OrderTableRequest(3, false));
        OrderTableResponse invalidOrderTable2 = orderTableService.create(new OrderTableRequest(5, false));
        List<OrderTableRequest> orderTableRequests = Arrays.asList(new OrderTableRequest(invalidOrderTable1.getId()),
                new OrderTableRequest(invalidOrderTable2.getId()));
        TableGroupRequest request = new TableGroupRequest(orderTableRequests);

        // When & Then
        assertThatThrownBy(() -> tableGroupService.create(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("`단체 지정`할 `주문 테이블`이 `단체 지정`되어있으면 생성할 수 없다.")
    @Test
    void exceptionToCreateTableGroupWithRegisteredOrderTable() {
        // Given
        OrderTableResponse orderTable1 = orderTableService.create(new OrderTableRequest(0, true));
        OrderTableResponse orderTable2 = orderTableService.create(new OrderTableRequest(0, true));
        List<OrderTableRequest> orderTableRequests = Arrays.asList(new OrderTableRequest(orderTable1.getId()),
                new OrderTableRequest(orderTable2.getId()));
        TableGroupRequest request = new TableGroupRequest(orderTableRequests);
        tableGroupService.create(request);

        // When & Then
        assertThatThrownBy(() -> tableGroupService.create(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("`단체 지정`을 해제한다.")
    @Test
    void ungroupTableGroup() {
        // Given
        OrderTableResponse orderTable1 = orderTableService.create(new OrderTableRequest(0, true));
        OrderTableResponse orderTable2 = orderTableService.create(new OrderTableRequest(0, true));
        List<OrderTableRequest> orderTableRequests = Arrays.asList(new OrderTableRequest(orderTable1.getId()),
                new OrderTableRequest(orderTable2.getId()));
        TableGroupRequest request = new TableGroupRequest(orderTableRequests);
        TableGroupResponse tableGroup = tableGroupService.create(request);

        // When
        tableGroupService.ungroup(tableGroup.getId());
        OrderTable foundOrderTable1 = orderTableService.findById(orderTable1.getId());
        OrderTable foundOrderTable2 = orderTableService.findById(orderTable2.getId());

        // Then
        assertAll(
                () -> assertThat(foundOrderTable1.getTableGroup()).isNull(),
                () -> assertThat(foundOrderTable2.getTableGroup()).isNull()
        );
    }

    @DisplayName("`단체 지정`된 `주문 테이블`에서 `주문 상태`가 'COOKING' 이나 'MEAL' 이면 해제할 수 없다.")
    @Test
    void exceptionToUngroupTableGroup() {
        // Given
        OrderTableResponse orderTable1 = orderTableService.create(new OrderTableRequest(0, true));
        OrderTableResponse orderTable2 = orderTableService.create(new OrderTableRequest(0, true));
        List<OrderTableRequest> orderTableRequests = Arrays.asList(new OrderTableRequest(orderTable1.getId()),
                new OrderTableRequest(orderTable2.getId()));
        TableGroupRequest request = new TableGroupRequest(orderTableRequests);
        TableGroupResponse tableGroup = tableGroupService.create(request);

        ProductResponse 짬뽕 = productService.create(new ProductRequest("짬뽕", BigDecimal.valueOf(8_000)));
        ProductResponse 짜장면 = productService.create(new ProductRequest("짜장면", BigDecimal.valueOf(6_000)));
        MenuGroupResponse 신메뉴그룹 = menuGroupService.create(new MenuGroupRequest("신메뉴그룹"));
        MenuResponse 추천메뉴 = menuService.create(new MenuRequest("추천메뉴", BigDecimal.valueOf(14_000), 신메뉴그룹.getId(),
                Arrays.asList(new MenuProductRequest(짬뽕.getId(), 1L), new MenuProductRequest(짜장면.getId(), 1L)))
        );

        OrderMenuRequest orderMenuRequest = new OrderMenuRequest(추천메뉴.getId(), 1L);
        OrderRequest orderRequest = new OrderRequest(orderTable1.getId(), Collections.singletonList(orderMenuRequest));
        orderService.create(orderRequest);

        // When & Then
        assertThatThrownBy(() -> tableGroupService.ungroup(tableGroup.getId()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}