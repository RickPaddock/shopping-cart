package shop

object ShoppingCart {

  /////////////////////////////////
  // AVAILABLE ITEMS & DISCOUNTS //
  /////////////////////////////////

  // Base £ prices
  val BASE_PRICES: Map[String, Double] = Map(
    "Soup"  -> 0.65,
    "Bread" -> 0.80,
    "Milk"  -> 1.30,
    "Apples" -> 1.00
  )

  // Discount rules
  val DISCOUNT_RULES = List(
    Map(
      "discount" -> "Apples 10% off",
      "type" -> "reduction",
      "item" -> "Apples",
      "percent" -> 0.10
    ),
    Map(
      "discount" -> "Buy 2 tins of soup and get a loaf of bread for half price",
      "type" -> "bogof",
      "conditional_item" -> "Soup",
      "conditional_quantity" -> 2,
      "discount_item" -> "Bread",
      "discount_percent" -> 0.50
    )
  )


  //////////////////////////////
  // DQ / CLEANING FUNCTIONS  //
  //////////////////////////////

  // Return items in correct capitalised case
  def clean_items(items: List[String]) = {
      items.map(item => item.toLowerCase().capitalize)
  }

 

  /////////////////////////
  // DISCOUNT FUNCTIONS  //
  /////////////////////////
  
  def calc_reduction(base_prices: Map[String, Double], rule: Map[String, Any], items: List[String]): (Double, String) = {
    val item = rule("item").toString
    val percent = rule("percent").asInstanceOf[Double]  // 'Any' requires 'asInstanceOf[]'
    val discount = rule("discount").toString
    val count = items.count(_ == item) // count = items.count(item)
    var discount_val = 0.0 // var is mutable

    if (count > 0) {
      discount_val = base_prices(item) * percent * count
      (discount_val, f"$discount: ${(discount_val * 100).toInt}p")
    } else {
      (0.0, "")
    }
  }


  def calc_bogof(base_prices: Map[String, Double], rule: Map[String, Any], items: List[String]): (Double, String) = {
    var discount_val = 0.0 // perpare output format
    val discount_rule = rule("discount").toString
    // The condition on which the item will be applied (e.g. 2 x Soup)
    val conditional_item = rule("conditional_item").toString
    val conditional_quantity = rule("conditional_quantity").asInstanceOf[Int]
    // The item on which the discount will be applied (e.g. Bread * 0.5)
    val discount_item = rule("discount_item").toString
    val discount_percent = rule("discount_percent").asInstanceOf[Double]
    // Ensuring the quantity of items provided satifies the discont conditions 
    val count_conditional_item = items.count(_ == conditional_item)
    val count_discount_item = items.count(_ == discount_item)
    if (count_conditional_item < conditional_quantity || count_discount_item == 0){
      (0.0, "")
    } else {
      // The discount applies once for every MULTIPLE set of conditional_item
      // E.G. if there are 6 soups, and discount applied for evry 2, this returns 3
      val sets = (count_conditional_item / conditional_quantity).toInt  // Equivalent of pythons floor division '//'
      // The discount can only apply to the number of relevant items in the basket
      // E.G. 3 sets means there are 3 possible discounts to apply, so up to 3 Breads!
      val applicable = sets.min(count_discount_item)
      discount_val = Math.round(base_prices(discount_item) * discount_percent * applicable * 100.0) / 100.0
      (discount_val, f"$discount_rule: ${(discount_val * 100).toInt}p")
    } // End of if condition
  } // End of calc_bogof


  // Process all discount rules and return the total discount amount and a list of discount description strings.
  def calc_discounts(base_prices: Map[String, Double], discount_rules: List[Map[String, Any]], items: List[String]): (Double, List[String]) = {
      // var makes these mutable
      var total_discount = 0.0
      var descriptions = List[String]()
      // Loop through all possible discounts
      for (rule <- discount_rules) {
          // Pull the rule type and run the relevant function with the rule details, and the full list of our items
          val (discount_val, desc) = rule("type") match{
                                                          case "reduction" => calc_reduction(base_prices, rule, items)
                                                          case "bogof" => calc_bogof(base_prices, rule, items)
                                                          case _ => (0.0, "")  
                                                        } // End of Match
            
            if (discount_val > 0){
              total_discount += discount_val
              descriptions = descriptions.appended(desc)
            } // End of discount > 0

      } // End of loop
      (total_discount, descriptions)
    } // End of calc_discounts


  ////////////////////////
  // SUMMARY FUNCTIONS  //
  ////////////////////////

  // Calculate the subtotal from the list of items
  def calc_subtotal(base_prices: Map[String, Double], items: List[String]): Double = {
    items.map(item => base_prices(item)).sum
  }

  def generate_receipt(items: List[String]): Unit = {

    val subtotal = calc_subtotal(BASE_PRICES, items)
    val (discount_total, discount_descriptions) = calc_discounts(BASE_PRICES, DISCOUNT_RULES, items)
    val total = subtotal - discount_total

    printf("Subtotal: £%.2f\n".format(subtotal))

    if (discount_descriptions.nonEmpty) {
      discount_descriptions.foreach(println)
    } else {
      println("(No offers available)")
    }

   printf("Total price: £%.2f\n".format(total))
  } // end of function generate_receipt


  def main(args: Array[String]): Unit = {
    // ItemsRaw contains all cli arguments 
    val itemsRaw: List[String] = args.toList
    // Run the code only if shopping items have been provided
    if (itemsRaw.isEmpty) {
      println("You must include items when invoking the ShoppingCart:\n sbt 'runMain shop.ShoppingCart item1 item2 .... itemN' ")
    } else {
      // Force items to Propercase (capitalised) before generating receipt
      val items = clean_items(itemsRaw)
      generate_receipt(items)
    }
  } // Close main
} // Close ShoppingCart
