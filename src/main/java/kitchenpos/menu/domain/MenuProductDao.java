package kitchenpos.menu.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuProductDao extends JpaRepository<MenuProduct, Long> {
	List<MenuProduct> findAllByMenuId(Long menuId);
}