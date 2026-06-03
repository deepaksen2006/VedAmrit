package com.example.vedaahar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vedaahar.ui.theme.BeigeBorder
import com.example.vedaahar.ui.theme.Cream
import com.example.vedaahar.ui.theme.DarkForestGreen
import com.example.vedaahar.ui.theme.ForestGreen
import com.example.vedaahar.ui.theme.LightSage
import com.example.vedaahar.ui.theme.PureWhite
import com.example.vedaahar.ui.theme.SageGreen
import com.example.vedaahar.ui.theme.SoftBlueGray

private val StoreBackground = Color(0xFFFAF6EC)
private val StoreCard = Color(0xFFFFFCF5)
private val StoreImagePanel = Color(0xFFEAF0E0)
private val StoreGold = Color(0xFFE7C269)
private val StoreLine = Color(0xFFE3D7C4)
private val StoreMuted = Color(0xFF6A6F62)

private data class StoreProduct(
    val name: String,
    val benefit: String,
    val category: String,
    val price: Int,
    val rating: String
)

private val storeCategories = listOf(
    "All",
    "Medicine",
    "Immunity",
    "Digestion",
    "Hair Care",
    "Skin Care",
    "Wellness",
    "Daily Care"
)

private val storeProducts = listOf(
    StoreProduct("Ashwagandha", "Stress & immunity", "Medicine", 200, "4.6"),
    StoreProduct("Triphala", "Digestion", "Digestion", 150, "4.7"),
    StoreProduct("Brahmi", "Memory & focus", "Wellness", 250, "4.8"),
    StoreProduct("Amla Powder", "Vitamin C", "Immunity", 180, "4.8"),
    StoreProduct("Giloy", "Immunity booster", "Immunity", 220, "4.9"),
    StoreProduct("Neem Capsules", "Skin & detox", "Skin Care", 200, "4.6"),
    StoreProduct("Shatavari", "Hormonal balance", "Wellness", 300, "4.7"),
    StoreProduct("Guggulu", "Joint support", "Medicine", 350, "4.8"),
    StoreProduct("Chyawanprash", "Immunity", "Immunity", 400, "4.9"),
    StoreProduct("Aloe Vera Gel", "Skin hydration", "Skin Care", 120, "4.6"),
    StoreProduct("Neem Face Wash", "Acne control", "Skin Care", 150, "4.7"),
    StoreProduct("Raw Honey", "Immunity", "Daily Care", 300, "4.8"),
    StoreProduct("Herbal Shampoo", "Hair care", "Hair Care", 250, "4.8"),
    StoreProduct("Bhringraj Oil", "Hair growth", "Hair Care", 280, "4.9"),
    StoreProduct("Tulsi Drops", "Immunity", "Immunity", 150, "4.6"),
    StoreProduct("Rose Water", "Toner", "Daily Care", 180, "4.8"),
    StoreProduct("Arjuna Powder", "Heart health", "Medicine", 200, "4.8"),
    StoreProduct("Multani Mitti", "Face pack", "Skin Care", 100, "4.7"),
    StoreProduct("Herbal Toothpaste", "Oral care", "Daily Care", 120, "4.8"),
    StoreProduct("Essential Oil", "Relaxation", "Wellness", 500, "4.9")
)

@Composable
fun ShoppingScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    val quantities = remember { mutableStateMapOf<String, Int>() }
    val cartCount by remember {
        derivedStateOf { quantities.values.sum() }
    }
    val filteredProducts by remember(searchQuery, selectedCategory) {
        derivedStateOf {
            storeProducts.filter { product ->
                val matchesCategory = selectedCategory == "All" ||
                    product.category.equals(selectedCategory, ignoreCase = true) ||
                    product.benefit.contains(selectedCategory, ignoreCase = true)
                val matchesSearch = searchQuery.isBlank() ||
                    product.name.contains(searchQuery, ignoreCase = true) ||
                    product.category.contains(searchQuery, ignoreCase = true) ||
                    product.benefit.contains(searchQuery, ignoreCase = true)
                matchesCategory && matchesSearch
            }
        }
    }

    Surface(modifier = modifier.fillMaxSize(), color = StoreBackground) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(StoreBackground, Cream, LightSage.copy(alpha = 0.6f))))
        ) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                StoreTopBar(
                    cartCount = cartCount,
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    onBack = onBack
                )
                CategoryChips(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it }
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 14.dp, top = 14.dp, end = 14.dp, bottom = if (cartCount > 0) 104.dp else 22.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredProducts, key = { it.name }) { product ->
                        ProductCard(
                            product = product,
                            quantity = quantities[product.name] ?: 0,
                            onAdd = { quantities[product.name] = (quantities[product.name] ?: 0) + 1 },
                            onRemove = {
                                val nextQuantity = ((quantities[product.name] ?: 0) - 1).coerceAtLeast(0)
                                if (nextQuantity == 0) {
                                    quantities.remove(product.name)
                                } else {
                                    quantities[product.name] = nextQuantity
                                }
                            }
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = cartCount > 0,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = fadeIn(tween(180)) + expandVertically(),
                exit = fadeOut(tween(160)) + shrinkVertically()
            ) {
                BottomCartBar(cartCount = cartCount)
            }
        }
    }
}

@Composable
private fun StoreTopBar(
    cartCount: Int,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackButton(onClick = onBack)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp, end = 8.dp)
            ) {
                Text(
                    "Ayurvedic Store",
                    color = DarkForestGreen,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 25.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Medicines • Wellness • Natural Care",
                    color = SageGreen,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            CartIcon(cartCount = cartCount)
        }
        TextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .shadow(8.dp, RoundedCornerShape(18.dp), ambientColor = ForestGreen.copy(alpha = 0.06f), spotColor = ForestGreen.copy(alpha = 0.05f)),
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = null, tint = SageGreen, modifier = Modifier.size(20.dp))
            },
            placeholder = {
                Text("Search herbs, medicines, products", color = StoreMuted, fontSize = 14.sp)
            },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = PureWhite,
                unfocusedContainerColor = PureWhite,
                disabledContainerColor = PureWhite,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = ForestGreen
            )
        )
    }
}

@Composable
private fun CartIcon(cartCount: Int) {
    Box {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(PureWhite)
                .border(BorderStroke(1.dp, StoreLine), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.ShoppingCart, contentDescription = "Cart", tint = ForestGreen)
        }
        if (cartCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(21.dp)
                    .clip(CircleShape)
                    .background(StoreGold)
                    .border(BorderStroke(1.dp, PureWhite), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(cartCount.toString(), color = DarkForestGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CategoryChips(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        items(storeCategories.size) { index ->
            val category = storeCategories[index]
            val selected = category == selectedCategory
            val background by animateColorAsState(if (selected) ForestGreen else PureWhite, label = "chipBackground")
            val textColor by animateColorAsState(if (selected) PureWhite else ForestGreen, label = "chipText")
            Text(
                text = category,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(background)
                    .border(BorderStroke(1.dp, if (selected) ForestGreen else BeigeBorder), RoundedCornerShape(50))
                    .clickable { onCategorySelected(category) }
                    .padding(horizontal = 15.dp, vertical = 9.dp),
                color = textColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ProductCard(
    product: StoreProduct,
    quantity: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, label = "productPress")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(interactionSource = interactionSource, indication = null, onClick = {})
            .shadow(14.dp, RoundedCornerShape(22.dp), ambientColor = ForestGreen.copy(alpha = 0.11f), spotColor = ForestGreen.copy(alpha = 0.09f)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = StoreCard),
        border = BorderStroke(1.dp, Color(0xFFE8DDC8)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.74f)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                PureWhite,
                                Color(0xFFF8F6ED),
                                StoreImagePanel.copy(alpha = 0.62f)
                            )
                        )
                    ),
                contentAlignment = Alignment.BottomCenter
            ) {
                ProductImage(
                    product = product,
                    modifier = Modifier.offset(y = 12.dp)
                )
                ProductTag(
                    text = if (product.category in listOf("Medicine", "Digestion", "Immunity")) "MEDICINE" else "PRODUCT",
                    modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
                )
            }
            Column(
                modifier = Modifier.padding(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        product.name,
                        modifier = Modifier.weight(1f),
                        color = DarkForestGreen,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    RatingBadge(product.rating)
                }
                Text(
                    product.benefit,
                    color = StoreMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text("\u20B9${product.price}", color = Color(0xFF0F2319), fontSize = 19.sp, fontWeight = FontWeight.Black)
                    }
                    QuantityButton(quantity = quantity, onAdd = onAdd, onRemove = onRemove)
                }
            }
        }
    }
}

@Composable
private fun ProductImage(product: StoreProduct, modifier: Modifier = Modifier) {
    if (product.name == "Ashwagandha") {
        RealProductImage(
            drawableId = R.drawable.ashwagandha_powder,
            contentDescription = "Ashwagandha Powder",
            modifier = modifier
        )
        return
    }
    if (product.name == "Triphala") {
        RealProductImage(
            drawableId = R.drawable.triphala,
            contentDescription = "Triphala",
            modifier = modifier
        )
        return
    }
    if (product.name == "Brahmi") {
        RealProductImage(
            drawableId = R.drawable.brahmi,
            contentDescription = "Brahmi",
            modifier = modifier
        )
        return
    }
    if (product.name == "Amla Powder") {
        RealProductImage(
            drawableId = R.drawable.amla_powder,
            contentDescription = "Amla Powder",
            modifier = modifier
        )
        return
    }
    if (product.name == "Giloy") {
        RealProductImage(
            drawableId = R.drawable.giloy,
            contentDescription = "Giloy",
            modifier = modifier
        )
        return
    }
    if (product.name == "Neem Capsules") {
        RealProductImage(
            drawableId = R.drawable.neem_capsules,
            contentDescription = "Neem Capsules",
            modifier = modifier
        )
        return
    }
    if (product.name == "Shatavari") {
        RealProductImage(
            drawableId = R.drawable.shatavari,
            contentDescription = "Shatavari",
            modifier = modifier
        )
        return
    }
    if (product.name == "Guggulu") {
        RealProductImage(
            drawableId = R.drawable.guggulu,
            contentDescription = "Guggulu",
            modifier = modifier
        )
        return
    }
    if (product.name == "Chyawanprash") {
        RealProductImage(
            drawableId = R.drawable.chyawanprash,
            contentDescription = "Chyawanprash",
            modifier = modifier
        )
        return
    }
    if (product.name == "Aloe Vera Gel") {
        RealProductImage(
            drawableId = R.drawable.aloe_vera_gel,
            contentDescription = "Aloe Vera Gel",
            modifier = modifier
        )
        return
    }
    if (product.name == "Neem Face Wash") {
        RealProductImage(
            drawableId = R.drawable.neem_face_wash,
            contentDescription = "Neem Face Wash",
            modifier = modifier
        )
        return
    }
    if (product.name == "Raw Honey") {
        RealProductImage(
            drawableId = R.drawable.raw_honey,
            contentDescription = "Raw Honey",
            modifier = modifier
        )
        return
    }
    if (product.name == "Herbal Shampoo") {
        RealProductImage(
            drawableId = R.drawable.herbal_shampoo,
            contentDescription = "Herbal Shampoo",
            modifier = modifier
        )
        return
    }
    if (product.name == "Bhringraj Oil") {
        RealProductImage(
            drawableId = R.drawable.bhringraj_oil,
            contentDescription = "Bhringraj Oil",
            modifier = modifier
        )
        return
    }
    if (product.name == "Tulsi Drops") {
        RealProductImage(
            drawableId = R.drawable.tulsi_drops,
            contentDescription = "Tulsi Drops",
            modifier = modifier
        )
        return
    }
    if (product.name == "Rose Water") {
        RealProductImage(
            drawableId = R.drawable.rose_water,
            contentDescription = "Rose Water",
            modifier = modifier
        )
        return
    }
    if (product.name == "Arjuna Powder") {
        RealProductImage(
            drawableId = R.drawable.arjuna_powder,
            contentDescription = "Arjuna Powder",
            modifier = modifier
        )
        return
    }
    if (product.name == "Multani Mitti") {
        RealProductImage(
            drawableId = R.drawable.multani_mitti,
            contentDescription = "Multani Mitti",
            modifier = modifier
        )
        return
    }
    if (product.name == "Herbal Toothpaste") {
        RealProductImage(
            drawableId = R.drawable.herbal_toothpaste,
            contentDescription = "Herbal Toothpaste",
            modifier = modifier
        )
        return
    }
    if (product.name == "Essential Oil") {
        RealProductImage(
            drawableId = R.drawable.essential_oil,
            contentDescription = "Essential Oil",
            modifier = modifier
        )
        return
    }

    val accent = when (product.category) {
        "Immunity" -> Color(0xFF8FAF3C)
        "Digestion" -> Color(0xFFC58B3B)
        "Hair Care" -> Color(0xFF315B44)
        "Skin Care" -> Color(0xFF77A878)
        "Daily Care" -> Color(0xFFD4A84E)
        else -> ForestGreen
    }
    Box(
        modifier = modifier
            .size(width = 112.dp, height = 128.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(PureWhite)
            .border(BorderStroke(1.dp, StoreLine), RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 62.dp, height = 84.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.95f), accent.copy(alpha = 0.72f))))
                .border(BorderStroke(1.dp, PureWhite.copy(alpha = 0.75f)), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = product.name.take(2).uppercase(),
                color = PureWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .width(66.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFFF5E8CE)),
            contentAlignment = Alignment.Center
        ) {
            Text("veda", color = ForestGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RealProductImage(
    drawableId: Int,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = drawableId),
        contentDescription = contentDescription,
        modifier = modifier
            .size(width = 142.dp, height = 164.dp),
        contentScale = ContentScale.Fit
    )
}

@Composable
private fun ProductTag(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(PureWhite.copy(alpha = 0.94f))
            .border(BorderStroke(1.dp, BeigeBorder.copy(alpha = 0.7f)), RoundedCornerShape(50))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        color = SageGreen,
        fontSize = 8.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.1.sp
    )
}

@Composable
private fun RatingBadge(rating: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xFFF6EBD5))
            .padding(horizontal = 5.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Star, contentDescription = null, tint = StoreGold, modifier = Modifier.size(10.dp))
        Text(rating, color = Color(0xFF8D6C20), fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun QuantityButton(
    quantity: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    if (quantity == 0) {
        OutlinedButton(
            onClick = onAdd,
            modifier = Modifier.height(34.dp),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color(0xFF67A62C)),
            contentPadding = PaddingValues(horizontal = 17.dp),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = PureWhite, contentColor = Color(0xFF4B951F))
        ) {
            Text("ADD", fontSize = 12.sp, fontWeight = FontWeight.Black)
        }
    } else {
        Row(
            modifier = Modifier
                .height(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ForestGreen),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepperButton(text = "-", onClick = onRemove)
            Text(
                quantity.toString(),
                modifier = Modifier.width(22.dp),
                color = PureWhite,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp
            )
            StepperButton(text = "+", onClick = onAdd)
        }
    }
}

@Composable
private fun StepperButton(text: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(34.dp)) {
        Text(text, color = PureWhite, fontSize = 18.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun BottomCartBar(cartCount: Int) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .shadow(18.dp, RoundedCornerShape(20.dp), ambientColor = ForestGreen.copy(alpha = 0.2f), spotColor = ForestGreen.copy(alpha = 0.16f)),
        shape = RoundedCornerShape(20.dp),
        color = ForestGreen,
        border = BorderStroke(1.dp, Color(0xFF7CAA52))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$cartCount items added", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("View Cart", color = PureWhite, fontWeight = FontWeight.Black, fontSize = 15.sp)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Filled.ShoppingCart, contentDescription = null, tint = PureWhite, modifier = Modifier.size(18.dp))
            }
        }
    }
}
