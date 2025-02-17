import argparse
from typing import List, Dict, Tuple # Not required in Python version >3.9


###################################
### AVAILABLE ITEMS & DISCOUNTS ###
###################################
# These mimic json files. Easy to ad new items and discounts
# Considered using dataclasses/pydantic but seemed overkill for this simple code

# Prices in £
BASE_PRICES = {
        "Soup": 0.65,
        "Bread": 0.80,
        "Milk": 1.30,
        "Apples": 1.00
    }


# Discount rules
DISCOUNT_RULES = [
        {
            "discount": "Apples 10% off",
            "type": "reduction",
            "item": "Apples",
            "percent": 10,
        },
        {
            "discount": "Buy 2 tins of soup and get a loaf of bread for half price",
            "type": "bogof",
            "conditional_item": "Soup",
            "conditional_quantity": 2,
            "discount_item": "Bread",
            "discount_percent": 0.50,
        },
    ]


#############################
### DQ/CLENSING FUNCTIONS ###
#############################

# >>>>> Note: data type checking could be enforced with PYDANTIC instead <<<<<<
def check_base_prices(base_prices: dict) -> None:
    """ Raise an exception if BASE_PRICES is not in correct format """
    errors_found = [] # Capture all errors so multiple can be reported at once
    for item, price in base_prices.items():
        # Item case check
        if item != item.capitalize():
            errors_found.append(f"The base price key '{item}' is not capitalised correctly.")
        # Price format check
        if not isinstance(price, (int, float)):
            errors_found.append(f"The base price '{item}: {price}' is not a valid number.")
        if isinstance(price, (int, float)) and price < 0:
            errors_found.append(f"The base price '{item}: {price}' can't be negative.")
        if isinstance(price, (int, float)) and round(price, 2) != price:
            errors_found.append(f"The base price '{item}: {price}' has more than 2.d.p.")
        
    if len(errors_found) > 0:
        raise ValueError(f"Data Quality Error: 'base_prices' issues have been found:\n" 
                            + "\n".join(errors_found))


def check_items_exist(items: List[str], base_prices: dict) -> None:
    """ Raise exception if item requested does not exist """
    items_missing = [item for item in items if item not in base_prices.keys()]

    if len(items_missing) > 0:
        raise ValueError(f"Your requested items are not available:\n" + "\n".join(items_missing))


def clean_items(items: List[str]) -> List[str]:
    """ Return items in correct capitalised case """
    return [item.capitalize() for item in items]


##########################
### DISCOUNT FUNCTIONS ###
##########################

def calc_reduction(rule: Dict, items: List[str]) -> Tuple[float, str]:
    """ Apply a percentage reduction discount.
        Parameters:
            - rule: details of 1 specific discount offer.
            - items: list of items provided by user.
        Returns:
            (discount_amount, discount_description)
    """
    item = rule["item"]
    percent = rule["percent"]
    # count how many items are eligible for discount
    count = items.count(item)
    # calculte discount for item multipled by total number of eligble items
    if count > 0:
        discount = round(BASE_PRICES[item] * (percent / 100.0) * count, 2)
        return discount, f"{rule['discount']}: {int(discount * 100)}p"
    return 0.0, ""


def calc_bogof(rule: Dict, items: List[str]) -> Tuple[float, str]:
    """ Apply "buy X get Y discount" offer.
        Parameters:
            - rule: details of 1 specific discount offer.
            - items: list of items provided by user.
        Returns:
            (discount_amount, discount_description)
    """
    # The condition on which the item will be applied (e.g. 2 x Soup)
    conditional_item = rule["conditional_item"]
    conditional_quantity = rule["conditional_quantity"]
    # The item on which the discount will be applied (e.g. Bread * 0.5)
    discount_item = rule["discount_item"]
    discount_percent = rule["discount_percent"]
    # Ensuring the quantity of items provided satifies the discont conditions 
    count_conditional_item = items.count(conditional_item)
    count_discount_item = items.count(discount_item)

    # The discount applies once for every MULTIPLE set of conditional_item
    # E.G. if there are 6 soups, and discount applied for evry 2, this returns 3
    sets = count_conditional_item // conditional_quantity
    # The discount can only apply to the number of relevant items in the basket
    # E.G. 3 sets means there are 3 possible discounts to apply, so up to 3 Breads!
    applicable = min(sets, count_discount_item)

    if sets > count_discount_item:
        possible = sets - count_discount_item
        print("You are not taking advantage of possible offers!")
        print(f"You have {count_conditional_item} {conditional_item}. You are eligible for {discount_percent} discount on {possible} more {discount_item}\n")

    # No valid offers returns default values
    if count_conditional_item < conditional_quantity or count_discount_item == 0:
        return 0.0, ""

    # Apply discount to all applicable items
    discount = round(BASE_PRICES[discount_item] * discount_percent * applicable, 2)

    return discount, f"{rule['discount']}: {int(discount * 100)}p"



def calc_discounts(items: List[str]) -> Tuple[float, List[str]]:
    """
    Process all discount rules and return the total discount amount
    and a list of discount description strings.
    """
    total_discount = 0.0
    descriptions = []
    # Loop through all possible discounts
    for rule in DISCOUNT_RULES:
        if rule["type"] == "reduction":
            discount, desc = calc_reduction(rule, items)
        elif rule["type"] == "bogof":
            discount, desc = calc_bogof(rule, items)
        else:
            discount, desc = 0.0,
        if discount > 0:
            total_discount += discount
            descriptions.append(desc)
    return total_discount, descriptions


#########################
### SUMMARY FUNCTIONS ###
#########################

def calc_subtotal(items: List[str]) -> float:
    """ Calculate the subtotal from the list of items """
    return round(sum(BASE_PRICES[item] for item in items), 2)


def calc_itemised(items: List[str]) -> None:
    """ Print price of each individual item """
    cumulative = 0
    print("     ITEM  PRICE--CUMULATIVE")
    for n, item in enumerate(items, start=1):
        cumulative += BASE_PRICES[item]
        print(f"  {n}. {item}: £{BASE_PRICES[item]:.2f}--£{cumulative:.2f}")


def generate_receipt(items: List[str], Itemised=False) -> None:
    """ Print reciept. If itemized, then print individual items first """
    if Itemised:
        calc_itemised(items) 
        
    subtotal = calc_subtotal(items)
    discount_total, discount_descriptions = calc_discounts(items)
    total = round(subtotal - discount_total, 2)

    print(f"Subtotal: £{subtotal:.2f}")
    if discount_descriptions:
        for discount in discount_descriptions:
            print(discount)
    else:
        print("(No offers available)")
    print(f"Total price: £{total:.2f}")


def main():
    # Parse items from command line. argparse makes this easier to include '-i'
    parser = argparse.ArgumentParser(description="Shopping Cart CLI")
    parser.add_argument("items", nargs="*", help="List of items to purchase")
    parser.add_argument("-i", "--Itemised", action="store_true", help="Print Itemised receipt")

    args = parser.parse_args()

    # This DQ check would normally be done in the residing Database, not here
    check_base_prices(BASE_PRICES)

    if not args.items:
        print("Syntax must be: python <code_name.py> item1 item2 item3 ...")
    else:
        items = clean_items(args.items)
        check_items_exist(items, BASE_PRICES)
        generate_receipt(items, Itemised=args.Itemised)


if __name__ == "__main__":
    main()
