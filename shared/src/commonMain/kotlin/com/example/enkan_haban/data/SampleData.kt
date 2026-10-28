package com.example.enkan_haban.data

import com.example.enkan_haban.domain.Category
import com.example.enkan_haban.domain.Condition
import com.example.enkan_haban.domain.ListingType
import com.example.enkan_haban.domain.Product
import com.example.enkan_haban.domain.Seller

/** Demo content used until the backend exists. */
object SampleData {
    val categories = listOf(
        Category("phones", "Phones", "📱", "ተሌፎን"),
        Category("electronics", "Electronics", "💻", "ኤሌክትሮኒክስ"),
        Category("fashion", "Fashion", "👗", "ክዳውንቲ"),
        Category("home", "Home", "🛋️", "ኣቑሑ ገዛ"),
        Category("vehicles", "Vehicles", "🚗", "መኻይን"),
        Category("property", "Property", "🏠", "ኣባይቲ"),
        Category("beauty", "Beauty", "💄", "ጽባቐ"),
        Category("groceries", "Groceries", "🛒", "መግቢ"),
    )

    private val officialStore = Seller("store", "EnkanHaban Store", "+251 900 000 000", isVerified = true)
    private val abebe = Seller("s1", "Abebe Electronics", "+251 911 223 344", isVerified = true)
    private val sara = Seller("s2", "Sara K.", "+251 922 556 677")
    private val dawit = Seller("s3", "Dawit Motors", "+251 933 778 899", isVerified = true)

    val products = listOf(
        Product(
            id = "p1", title = "Samsung Galaxy A55 128GB", price = 2_450_000, oldPrice = 2_800_000,
            description = "6.6\" AMOLED display, 50MP camera, 5000mAh battery. 1-year warranty.",
            categoryId = "phones", condition = Condition.NEW, location = "Addis Ababa",
            seller = officialStore, listingType = ListingType.STORE, rating = 4.6,
        ),
        Product(
            id = "p2", title = "iPhone 13 – 128GB, clean", price = 3_900_000,
            description = "Used for one year, battery health 89%, no scratches. Comes with charger.",
            categoryId = "phones", condition = Condition.USED, location = "Bole, Addis Ababa",
            seller = sara, listingType = ListingType.CLASSIFIED,
        ),
        Product(
            id = "p3", title = "HP EliteBook 840 G6, i5, 16GB RAM", price = 3_200_000, oldPrice = 3_650_000,
            description = "Refurbished business laptop, 512GB SSD, backlit keyboard.",
            categoryId = "electronics", condition = Condition.REFURBISHED, location = "Addis Ababa",
            seller = officialStore, listingType = ListingType.STORE, rating = 4.4,
        ),
        Product(
            id = "p4", title = "55\" Smart TV 4K UHD", price = 4_100_000,
            description = "Android TV with Netflix and YouTube built in. Wall mount included.",
            categoryId = "electronics", condition = Condition.NEW, location = "Piassa, Addis Ababa",
            seller = abebe, listingType = ListingType.CLASSIFIED,
        ),
        Product(
            id = "p5", title = "Men's Leather Sneakers", price = 350_000, oldPrice = 480_000,
            description = "Genuine leather, sizes 40–45. Comfortable everyday wear.",
            categoryId = "fashion", condition = Condition.NEW, location = "Addis Ababa",
            seller = officialStore, listingType = ListingType.STORE, rating = 4.2,
        ),
        Product(
            id = "p6", title = "Traditional Habesha Kemis", price = 650_000,
            description = "Handwoven cotton with tibeb embroidery. Made to order in 1 week.",
            categoryId = "fashion", condition = Condition.NEW, location = "Shiro Meda, Addis Ababa",
            seller = sara, listingType = ListingType.CLASSIFIED,
        ),
        Product(
            id = "p7", title = "3-Seater Fabric Sofa", price = 2_200_000,
            description = "Grey fabric sofa, 2 years old, very good condition. Buyer collects.",
            categoryId = "home", condition = Condition.USED, location = "CMC, Addis Ababa",
            seller = sara, listingType = ListingType.CLASSIFIED,
        ),
        Product(
            id = "p8", title = "Electric Kettle 1.7L", price = 120_000, oldPrice = 150_000,
            description = "Stainless steel, auto shut-off, 2200W.",
            categoryId = "home", condition = Condition.NEW, location = "Addis Ababa",
            seller = officialStore, listingType = ListingType.STORE, rating = 4.7,
        ),
        Product(
            id = "p9", title = "Toyota Vitz 2012, automatic", price = 120_000_000,
            description = "Low mileage, full service history, plate code 3.",
            categoryId = "vehicles", condition = Condition.USED, location = "Adama",
            seller = dawit, listingType = ListingType.CLASSIFIED,
        ),
        Product(
            id = "p10", title = "2-Bedroom Apartment for Rent", price = 2_500_000,
            description = "Monthly rent. 3rd floor, water tank, parking. Near Megenagna.",
            categoryId = "property", condition = Condition.NEW, location = "Megenagna, Addis Ababa",
            seller = dawit, listingType = ListingType.CLASSIFIED,
        ),
        Product(
            id = "p11", title = "Shea Butter Body Lotion 400ml", price = 45_000,
            description = "Moisturizing lotion for dry skin.",
            categoryId = "beauty", condition = Condition.NEW, location = "Addis Ababa",
            seller = officialStore, listingType = ListingType.STORE, rating = 4.5,
        ),
        Product(
            id = "p12", title = "Teff Flour (Magna) 10kg", price = 140_000, oldPrice = 155_000,
            description = "Premium white teff flour, freshly milled.",
            categoryId = "groceries", condition = Condition.NEW, location = "Addis Ababa",
            seller = officialStore, listingType = ListingType.STORE, rating = 4.8,
        ),
    )
}
