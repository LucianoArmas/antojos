package antojos.ecommerce.products;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import antojos.ecommerce.products.drink.Drink;
import antojos.ecommerce.products.drink.DrinkRepository;
import antojos.ecommerce.products.food.Food;
import antojos.ecommerce.products.food.FoodRepository;
import org.springframework.stereotype.Service;

@Service
public class ProductService {
  private final ProductRepository productRepository;
  private final FoodRepository foodRepository;
  private final DrinkRepository drinkRepository;

  public ProductService(ProductRepository productRepository, DrinkRepository drinkRepository, FoodRepository foodRepository) {
    this.productRepository = productRepository;
    this.drinkRepository = drinkRepository;
    this.foodRepository = foodRepository;
  }

  public Map<String, List<Product>> getProducts(String name){
    List<Product> productList;
    if (name.isBlank()){
      productList = productRepository.findAll();
    }else {
      productList = productRepository.findByNameContainingIgnoreCase(name);
    }

    Map<String, List<Product>> prodByType = new HashMap<>();

    for (Product product : productList){
      String type = product.getType();
      prodByType.computeIfAbsent(type, k -> new ArrayList<>()).add(product);
    }

    return prodByType;
  }

  public List<Food> getFoods(){
    return foodRepository.findAll();
  }

  public List<Drink> getDrinks(){
    return drinkRepository.findAll();
  }


  public Product getProductById(Long id){
    return productRepository.findById(id).orElse(null);
  }


  public void addProduct(Product product){
    productRepository.save(product);
  }

  public void updateProduct(Product product){
    //AGREGAR MODIFICACIONES DE LOS ATRIBUTOS
    productRepository.save(product);
  }

  public void deleteProduct(Long id){
    //AGREGAR VALIDACION DE EXISTENCIA
    productRepository.deleteById(id);
  }
}
