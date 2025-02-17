package shop

// TODO: Make inputs like base_prices global so they can be shared

class PriceSummaryTest extends munit.FunSuite {

  test("clean_items() should capitalize the first letter of each word") {
    val input = List("apple", "BREAD", "Car", "hOLIDAY")
    val expected_results = List("Apple", "Bread", "Car", "Holiday")
    assertEquals(ShoppingCart.clean_items(input), expected_results)
  }


  test("calc_subtotal() sums prices of given products correctly, ignoring missing items") {
    val BASE_PRICES = Map(
                          "Soup" -> 0.65,
                          "Bread" -> 0.80,
                          "Milk" -> 1.30,
                          "Apples" -> 1.00,
                          "neg_val" -> -100.00)
    
    // Sums all values correctly
    val input_items1 = List("Soup", "Bread", "Milk", "Apples")
    assertEquals(ShoppingCart.calc_subtotal(BASE_PRICES, input_items1), 3.75)

    // Missing items returns 0. Assumed missing items dealt with prior to this function
    val input_items2 = List("missing_item")
    assertEquals(ShoppingCart.calc_subtotal(BASE_PRICES, input_items2), 0.0)

    // Duplicate items and negative values
    val input_items3 = List("Soup", "Soup", "Bread", "Apples", "Milk", "missing_item", "neg_val")
    assertEquals(ShoppingCart.calc_subtotal(BASE_PRICES, input_items3), -95.6)
  }


  test("calc_reduction() applies discounts correcly") {
    val BASE_PRICES = Map(
                          "Soup" -> 0.65,
                          "Bread" -> 0.80,
                          "Milk" -> 1.30,
                          "Apples" -> 1.00)

    val DISCOUNT_RULES_map = Map(
                              "discount" -> "Apples 10% off",
                              "type" -> "reduction",
                              "item" -> "Apples",
                              "percent" -> 0.10
                            )

    // returns correct value for 1 discounted item
    val input_items1 = List("Apples")
    assertEquals(ShoppingCart.calc_reduction(BASE_PRICES, DISCOUNT_RULES_map, input_items1), (0.1, "Apples 10% off: 10p"))

    // returns correct value for multiple discounted items
    val input_items2 = List("Apples", "Apples", "Apples", "Apples")
    assertEquals(ShoppingCart.calc_reduction(BASE_PRICES, DISCOUNT_RULES_map, input_items2), (0.4, "Apples 10% off: 40p"))

    val input_items3 = List("None")
    assertEquals(ShoppingCart.calc_reduction(BASE_PRICES, DISCOUNT_RULES_map, input_items3), (0.0, ""))
  }


  test("calc_bogof() applies discounts correcly") {
    val BASE_PRICES = Map(
                          "Soup" -> 0.65,
                          "Bread" -> 0.80,
                          "Milk" -> 1.30,
                          "Apples" -> 1.00,
                          "neg_val" -> -100.00)

    val DISCOUNT_RULES_map = Map(
                                "discount" -> "Buy 2 tins of soup and get a loaf of bread for half price",
                                "type" -> "bogof",
                                "conditional_item" -> "Soup",
                                "conditional_quantity" -> 2,
                                "discount_item" -> "Bread",
                                "discount_percent" -> 0.50
                              )

    // returns correct value for eligible discounted items
    val input_items1 = List("Soup", "Soup", "Bread")
    assertEquals(ShoppingCart.calc_bogof(BASE_PRICES, DISCOUNT_RULES_map, input_items1), (0.4, "Buy 2 tins of soup and get a loaf of bread for half price: 40p"))

    // returns correct value for multiple discounted items
    val input_items2 = List("Soup", "Soup", "Bread", "Soup", "Soup", "Bread")
    assertEquals(ShoppingCart.calc_bogof(BASE_PRICES, DISCOUNT_RULES_map, input_items2), (0.8, "Buy 2 tins of soup and get a loaf of bread for half price: 80p"))

    // Non-matching items return 0.0 and missin
    val input_items3 = List("None")
    assertEquals(ShoppingCart.calc_bogof(BASE_PRICES, DISCOUNT_RULES_map, input_items3), (0.0, ""))

    // Qualifying conditional items return nothing if there is no discounted item available (i.e. 2 soups but no bread)
    val input_items4 = List("Soup", "Soup")
    assertEquals(ShoppingCart.calc_bogof(BASE_PRICES, DISCOUNT_RULES_map, input_items4), (0.0, ""))

    // The required discounted items exist (i.e. 1 bread) but not enough qualifying conditional items exist (i.e. 1 soup not 2) returns nothing
    val input_items5 = List("Soup", "Bread")
    assertEquals(ShoppingCart.calc_bogof(BASE_PRICES, DISCOUNT_RULES_map, input_items5), (0.0, ""))
  }


  // WAS STRUGGLING TO MOCK THE RESULTS OF TWO FUNCTIONS calc_bogof AND calc_reduction //

  // test("calc_discounts() should return total discount value and all discount text") {
        // calc_reduction = (0.4, "Apples 10% off: 40p")
        // calc_bogof = (0.8, "Buy 2 tins of soup and get a loaf of bread for half price: 80p")
  //   assertEquals(ShoppingCart.calc_discounts(input), expected_results)
  // }

} // End of PriceSummaryTests


