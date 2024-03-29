package antojos.ecommerce.order;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;

import antojos.ecommerce.orderLine.OrderLine;
import antojos.ecommerce.products.Product;
import antojos.ecommerce.products.ProductService;
import antojos.ecommerce.user.User;
import antojos.ecommerce.user.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.swing.text.DateFormatter;


@Controller
@RequestMapping("/orders")
public class OrderController {
  private OrderService orderService;

  private ProductService productService;

  private UserService userService;

  public OrderController(OrderService orderService, ProductService productService, UserService userService) {
    this.orderService = orderService;
    this.productService = productService;
    this.userService = userService;
  }

  @GetMapping("/cart")
  public String viewCart(Model model, HttpSession session){
    Order order = (Order) session.getAttribute("orderPending");
//    List<OrderLine> orderLineList = orderService.getOrderLinesFromSession(session);
    List<OrderLine> orderLineList = order.getOrderLineList();

    model.addAttribute("orderLines",orderLineList );
    return "/order/orderLines";
  }

  @PostMapping("/addToCart")
  public String addToCart(@RequestParam Long id, HttpSession session){
    Product product = productService.getProductById(id);
    Order order = (Order) session.getAttribute("orderPending");
    orderService.addProductToOrder(product, order, session);
    return "redirect:/";
  }

  @PostMapping("/addProd")
  public String addProd(@RequestParam Long numbOL, @RequestParam Long codOrder, HttpSession session){
    return processOrderProd(numbOL, codOrder, session, true);
  }

  @PostMapping("/deleteProd")
  public String deleteProd(@RequestParam Long numbOL, @RequestParam Long codOrder, HttpSession session){
    return processOrderProd(numbOL, codOrder, session, false);
  }

  private String processOrderProd(Long numbOL, Long codOrder, HttpSession session, boolean isAdd){
    OrderLine orderLine = orderService.getOrderLineByNumbAndCodeOrder(numbOL, codOrder);
    Order order = orderService.getOrderByCod(codOrder);

    if (orderLine != null){
      if (isAdd){
        orderService.addProdToOrderFromCart(session, order, orderLine);
      }else {
         orderService.deleteProdToOrderFromCart(session, order, orderLine);
//        orderService.updateProdStock(orderLine.getProduct().getId(), orderLine.getProduct().getStock()+1);
      }
    }

//    if(flag_thereIsStock){
//      orderService.updateProdStock(orderLine.getProduct().getId(), orderLine.getProduct().getStock()-1);
//    }

    return "/order/orderLines";
  }



  @PostMapping("/acceptOrder/{orderCod}")
  private String acceptOrder(@PathVariable Long orderCod, HttpSession session, Model model){
    Order order = orderService.getOrderByCod(orderCod);
    boolean flag_StockAccepted = true;
    Product prodStockNoAccepted=null;

    for (OrderLine ol: order.getOrderLineList()) {
      if(orderService.verifyProdStock(ol.getProduct(), ol.getQuantityProds())){
        orderService.updateProdStock(ol.getProduct().getId(), (ol.getProduct().getStock() - ol.getQuantityProds()));
      }else{
        flag_StockAccepted = false;
        prodStockNoAccepted = ol.getProduct(); //AUN NO SE COMO IMPLEMENTARLO - XQ DIRECTAMENTE NO PASA X EL FALSO, XQ SI VE Q NO HAY STOCK NO TE DEJA PONER EL PROD EN EL CART
        break;
      }
    }

    if(flag_StockAccepted){
      orderService.acceptOrder(order, session);
      return "redirect:/";
    }else{
      model.addAttribute("errorProdStock", prodStockNoAccepted);
      return "/order/orderLines";
    }

  }

@GetMapping("ordersAccepted") //MEPA Q LO TENGO Q ELIMINAR A ESTE METODO
public String getOrdersAccepted(Model model, HttpSession session){
  User user = (User) session.getAttribute("user");

  List<Order> orderList = orderService.getOrdersByUserAndState(user, "accepted");

  model.addAttribute("ordersAccepted", orderList);

  return "ordersAccepted(ELIMINAR-QUIZAS)";
}

private Map<String, List<Order>> getOrdersMap(List<Order> orderList){
    return orderService.divideOrdersByStatus(orderList);
}


@GetMapping("orderList")
public String getOrderList(Model model, HttpSession session){

User user = (User) session.getAttribute("user");

List<Order> orderListAccepted = orderService.getOrdersByUserAndState(user, "accepted");
List<Order> orderListCancelled = orderService.getOrdersByUserAndState(user, "cancelled");
List<Order> orderListDelivered = orderService.getOrdersByUserAndState(user, "delivered");

model.addAttribute("ordersAccepted", orderListAccepted);
model.addAttribute("ordersCancelled", orderListCancelled);
model.addAttribute("ordersDelivered", orderListDelivered);

return "/order/orderList";

}


@GetMapping("/orderListAdmin")
public String getOrderListAdmin(Model model){

  List<Order> orderList = orderService.getAllOrders();
  Map<String, List<Order>> ordersMap = getOrdersMap(orderList);

  List<Order> ordersAccepted =  ordersMap.get("accepted");
  List<Order> ordersCancelled = ordersMap.get("cancelled");
  List<Order> ordersDelivered = ordersMap.get("delivered");



  model.addAttribute("orderListAccepted", ordersAccepted);
  model.addAttribute("orderListCancelled", ordersCancelled);
  model.addAttribute("orderListDelivered", ordersDelivered);

  return "/order/orderListAdmin";
}



private Date formatter(String stringToDate){
  DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  LocalDate date = LocalDate.parse(stringToDate, dateFormatter);
  return java.sql.Date.valueOf(date);
}

@GetMapping("/search")
public String searchOrder(HttpSession session,Model model, @RequestParam("userDni") String userDni, @RequestParam("dateFrom") String dateFrom, @RequestParam("dateTo") String dateTo){
  User user = null;
  List<Order> orderList = null;
  Date orderDateFrom;
  Date orderDateTo;

  if (dateFrom.isBlank()){
    orderDateFrom = formatter("2000-01-01");
  }else {
    orderDateFrom = formatter(dateFrom);
  }


  if (dateTo.isBlank()){
    LocalDate today = (LocalDate.now());
    orderDateTo = java.sql.Date.valueOf(today);
  }else {
    orderDateTo = formatter(dateTo);
  }


  if (userDni.isBlank()){
    orderList = orderService.getOrdersBetweenDates(orderDateFrom, orderDateTo);
  }else{
    user = userService.getUserByDni(userDni);
    orderList = orderService.getByUserAndDate(user, orderDateFrom, orderDateTo);
  }

  Map<String, List<Order>> ordersMap = orderService.divideOrdersByStatus(orderList);

  List<Order> ordersAccepted = ordersMap.get("accepted");
  List<Order> ordersCancelled = ordersMap.get("cancelled");
  List<Order> ordersDelivered = ordersMap.get("delivered");



  model.addAttribute("orderListAccepted", ordersAccepted);
  model.addAttribute("orderListCancelled", ordersCancelled);
  model.addAttribute("orderListDelivered", ordersDelivered);

  return "/order/orderListAdmin";
}






@PostMapping("/modStatusAdmin")
public String modOrderStatusAdmin(@RequestParam Long cod, @RequestParam String orderStatus){
    Order order = orderService.getOrderByCod(cod);
    orderService.updateOrder(order, orderStatus);

    return "redirect:/orders/orderListAdmin";
}




@PostMapping("/cancel")
public String cancelOrder(@RequestParam Long cod){
    orderService.cancelOrder(cod);
    return "redirect:/orders/orderList";
}



  @PostMapping("/delete")
  public String deleteOrder(@RequestParam Long cod){
    orderService.deleteOrder(cod);
    return "redirect:/shoppings/list";
  }




//  @GetMapping("/list")
//  public String listShops(Model model){
//    List<Order> shops = orderService.getAllOrders();
//    model.addAttribute("orders", shops);
//    return "shoppings/list";
//  }
//
//
//
//  @GetMapping("/add")
//  public String addShopForm(Model model){
//    model.addAttribute("order", new Order());
//    return "shoppings/add";
//  }
//  @PostMapping("/add")
//  public String addShop(@ModelAttribute Order shop){
//    orderService.addOrder(shop);
//    return "redirect:/shoppings/list";
//  }
//
//
//  @GetMapping("/edit/{cod}")
//  public String editShopForm(@PathVariable Long cod, Model model){
//    Order shop = orderService.getOrderByCod(cod);
//    model.addAttribute("order", shop);
//    return "shoppings/edit";
//  }
//  @PostMapping("/edit/{cod}")
//  public String editShop(@PathVariable Long cod, @ModelAttribute Order order){
//    orderService.updateShopping(order);
//    return "redirect:/shoppings/list";
//  }




}
