package antojos.ecommerce.order;

import java.util.List;

import antojos.ecommerce.orderLine.OrderLine;
import antojos.ecommerce.products.Product;
import antojos.ecommerce.products.ProductService;
import antojos.ecommerce.user.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


@Controller
@RequestMapping("/orders")
public class OrderController {
  private OrderService orderService;

  private ProductService productService;

  public OrderController(OrderService orderService, ProductService productService) {
    this.orderService = orderService;
    this.productService = productService;
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
        prodStockNoAccepted = ol.getProduct(); //AUN NO SE COMO IMPLEMENTARLO
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

@GetMapping("ordersAccepted")
public String getOrdersAccepted(Model model, HttpSession session){
  User user = (User) session.getAttribute("user");

  List<Order> orderList = orderService.getOrdersByUserAndState(user, "accepted");

  model.addAttribute("ordersAccepted", orderList);

  return "/users/ordersAccepted";
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


  //TENDRIA Q CAMBIAR ESTE METODO, TANTO EL NOMBRE COMO LO QUE HACE (ES USADO PARA CUANDO EL CLIENTE CANCELA EL PEDIDO)
  @GetMapping("/delete/{cod}")
  public String deleteShgop(@PathVariable Long cod){
    orderService.deleteShopping(cod);
    return "redirect:/shoppings/list";
  }

}
