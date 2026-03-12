package com.tradeops;

import com.tradeops.model.entity.*;
import com.tradeops.model.response.DeliveryAssignmentResponse;
import com.tradeops.repo.*;
import com.tradeops.service.impl.CourierServiceImpl;
import com.tradeops.service.impl.DispatcherServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@SpringBootTest
@Transactional
public class EpicB6DispatchAcceptanceTest {

    @Autowired
    private DispatcherServiceImpl dispatcherService;
    @Autowired
    private CourierServiceImpl courierService;
    @Autowired
    private OrderRepo orderRepo;
    @Autowired
    private CourierUserRepo courierUserRepo;
    @Autowired
    private InventoryItemRepo inventoryItemRepo;
    @Autowired
    private DeliveryAssignmentRepo assignmentRepo;
    @Autowired
    private ProductRepo productRepo;

    private Order testOrder;
    private CourierUser testCourier;
    private InventoryItem testInventory;

    @BeforeEach
    void setUp() {
        Product p = new Product();
        p.setName("Test Cargo");
        p.setBasePrice(BigDecimal.valueOf(100.0));
        p.setSku("CARGO-1");
        p = productRepo.save(p);

        testInventory = new InventoryItem();
        testInventory.setProduct(p);
        testInventory.setQtyOnHand(10);
        testInventory.setQtyReserved(2);
        inventoryItemRepo.save(testInventory);

        testOrder = new Order();
        testOrder.setOrderNumber("KG-DISPATCH-TEST");
        testOrder.setStatus(OrderStatus.NEW);

        OrderLine line = new OrderLine();
        line.setOrder(testOrder);
        line.setProduct(p);
        line.setQty(2);
        testOrder.setOrderLines(List.of(line));
        orderRepo.save(testOrder);

        testCourier = new CourierUser();
        testCourier.setName("John Doe");
        testCourier.setPhone("+996555000111");
        testCourier.setIsActive(true);
        courierUserRepo.save(testCourier);
    }

    @Test
    @WithMockUser(username = "dispatcher", authorities = "ROLE_DISPATCHER")
    void dispatcherCanAssignOrder() {
        DeliveryAssignmentResponse assignment = dispatcherService.assignCourierToOrder(testOrder.getId(), testCourier.getId());

        Assertions.assertEquals(DeliveryStatus.ASSIGNED, assignment.status());
        Assertions.assertEquals(OrderStatus.ASSIGNED, orderRepo.findById(testOrder.getId()).get().getStatus());
    }

    @Test
    @WithMockUser(username = "+996555000111", authorities = "ROLE_COURIER")
    void courierFullDeliveryFlow() {
        DeliveryAssignment assignment = new DeliveryAssignment();
        assignment.setOrder(testOrder);
        assignment.setCourier(testCourier);
        assignment.setStatus(DeliveryStatus.ASSIGNED);
        assignmentRepo.save(assignment);

        testOrder.setStatus(OrderStatus.ASSIGNED);
        orderRepo.save(testOrder);

        List<DeliveryAssignmentResponse> feed = courierService.getActiveAssignments();
        Assertions.assertEquals(1, feed.size());

        DeliveryAssignmentResponse accepted = courierService.acceptAssignment(assignment.getId());
        Assertions.assertEquals(DeliveryStatus.ON_PROGRESS, accepted.status());
        Assertions.assertEquals(OrderStatus.ON_PROGRESS, orderRepo.findById(testOrder.getId()).get().getStatus());
        Assertions.assertNotNull(accepted.acceptedAt());

        DeliveryAssignmentResponse completed = courierService.completeAssignment(assignment.getId());
        Assertions.assertEquals(DeliveryStatus.COMPLETED, completed.status());
        Assertions.assertEquals(OrderStatus.COMPLETED, orderRepo.findById(testOrder.getId()).get().getStatus());

        InventoryItem dbItem = inventoryItemRepo.findById(testInventory.getId()).get();
        Assertions.assertEquals(8, dbItem.getQtyOnHand(), "Hand quantity must be deducted");
        Assertions.assertEquals(0, dbItem.getQtyReserved(), "Reserved quantity must be cleared");
    }
}
