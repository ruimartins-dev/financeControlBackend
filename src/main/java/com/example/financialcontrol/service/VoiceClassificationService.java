package com.example.financialcontrol.service;

import com.example.financialcontrol.dto.TransactionDraftDto;
import com.example.financialcontrol.dto.VoiceRequestDto;
import com.example.financialcontrol.entity.Category;
import com.example.financialcontrol.entity.Subcategory;
import com.example.financialcontrol.entity.TransactionType;
import com.example.financialcontrol.entity.User;
import com.example.financialcontrol.entity.Wallet;
import com.example.financialcontrol.repository.CategoryRepository;
import com.example.financialcontrol.repository.SubcategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for classifying voice/text input into a transaction draft.
 *
 * IMPORTANT: This service NEVER persists data to the database.
 * It only reads categories/subcategories to match against user input.
 *
 * The classification flow:
 * 1. Validate wallet ownership
 * 2. Parse text using RuleBasedTransactionParser
 * 3. Match category/subcategory against database entries
 * 4. Return a draft DTO for user confirmation
 */
@Service
public class VoiceClassificationService {

    private final WalletService walletService;
    private final RuleBasedTransactionParser parser;
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;

    // =====================================================
    // KEYWORD TO CATEGORY MAPPING
    // =====================================================
    // These keywords help detect categories from natural language.
    // Keywords can be in Portuguese or English, but they map to
    // the actual category names in the database (which are in English).

    private static final Map<String, String> KEYWORD_TO_CATEGORY = new HashMap<>();

    static {
        // Food & Dining related keywords (PT + EN)
        // DB Category: "Food & Dining"
        KEYWORD_TO_CATEGORY.put("supermercado", "Food & Dining");
        KEYWORD_TO_CATEGORY.put("supermercados", "Food & Dining");
        KEYWORD_TO_CATEGORY.put("supermarket", "Food & Dining");
        KEYWORD_TO_CATEGORY.put("mercado", "Food & Dining");
        KEYWORD_TO_CATEGORY.put("mercados", "Food & Dining");
        KEYWORD_TO_CATEGORY.put("grocery", "Food & Dining");
        KEYWORD_TO_CATEGORY.put("groceries", "Food & Dining");
        KEYWORD_TO_CATEGORY.put("restaurante", "Food & Dining");
        KEYWORD_TO_CATEGORY.put("restaurantes", "Food & Dining");
        KEYWORD_TO_CATEGORY.put("restaurant", "Food & Dining");
        KEYWORD_TO_CATEGORY.put("restaurants", "Food & Dining");
        KEYWORD_TO_CATEGORY.put("café", "Food & Dining");
        KEYWORD_TO_CATEGORY.put("cafe", "Food & Dining");
        KEYWORD_TO_CATEGORY.put("cafés", "Food & Dining");
        KEYWORD_TO_CATEGORY.put("cafes", "Food & Dining");
        KEYWORD_TO_CATEGORY.put("coffee", "Food & Dining");
        KEYWORD_TO_CATEGORY.put("almoço", "Food & Dining");
        KEYWORD_TO_CATEGORY.put("almoco", "Food & Dining");
        KEYWORD_TO_CATEGORY.put("lunch", "Food & Dining");
        KEYWORD_TO_CATEGORY.put("jantar", "Food & Dining");
        KEYWORD_TO_CATEGORY.put("jantares", "Food & Dining");
        KEYWORD_TO_CATEGORY.put("dinner", "Food & Dining");
        KEYWORD_TO_CATEGORY.put("comida", "Food & Dining");
        KEYWORD_TO_CATEGORY.put("comidas", "Food & Dining");
        KEYWORD_TO_CATEGORY.put("food", "Food & Dining");
        KEYWORD_TO_CATEGORY.put("alimentação", "Food & Dining");
        KEYWORD_TO_CATEGORY.put("alimentacao", "Food & Dining");
        KEYWORD_TO_CATEGORY.put("refeição", "Food & Dining");
        KEYWORD_TO_CATEGORY.put("refeicao", "Food & Dining");
        KEYWORD_TO_CATEGORY.put("refeições", "Food & Dining");
        KEYWORD_TO_CATEGORY.put("refeicoes", "Food & Dining");
        KEYWORD_TO_CATEGORY.put("fast food", "Food & Dining");
        KEYWORD_TO_CATEGORY.put("delivery", "Food & Dining");
        KEYWORD_TO_CATEGORY.put("entrega", "Food & Dining");

        // Transportation related keywords (PT + EN)
        // DB Category: "Transportation"
        KEYWORD_TO_CATEGORY.put("uber", "Transportation");
        KEYWORD_TO_CATEGORY.put("bolt", "Transportation");
        KEYWORD_TO_CATEGORY.put("taxi", "Transportation");
        KEYWORD_TO_CATEGORY.put("taxis", "Transportation");
        KEYWORD_TO_CATEGORY.put("táxi", "Transportation");
        KEYWORD_TO_CATEGORY.put("táxis", "Transportation");
        KEYWORD_TO_CATEGORY.put("gasolina", "Transportation");
        KEYWORD_TO_CATEGORY.put("gas", "Transportation");
        KEYWORD_TO_CATEGORY.put("fuel", "Transportation");
        KEYWORD_TO_CATEGORY.put("combustível", "Transportation");
        KEYWORD_TO_CATEGORY.put("combustivel", "Transportation");
        KEYWORD_TO_CATEGORY.put("transporte", "Transportation");
        KEYWORD_TO_CATEGORY.put("transportes", "Transportation");
        KEYWORD_TO_CATEGORY.put("transport", "Transportation");
        KEYWORD_TO_CATEGORY.put("transportation", "Transportation");
        KEYWORD_TO_CATEGORY.put("metro", "Transportation");
        KEYWORD_TO_CATEGORY.put("bus", "Transportation");
        KEYWORD_TO_CATEGORY.put("autocarro", "Transportation");
        KEYWORD_TO_CATEGORY.put("autocarros", "Transportation");
        KEYWORD_TO_CATEGORY.put("ônibus", "Transportation");
        KEYWORD_TO_CATEGORY.put("onibus", "Transportation");
        KEYWORD_TO_CATEGORY.put("estacionamento", "Transportation");
        KEYWORD_TO_CATEGORY.put("parking", "Transportation");
        KEYWORD_TO_CATEGORY.put("carro", "Transportation");
        KEYWORD_TO_CATEGORY.put("car", "Transportation");

        // Salary/Income related keywords (PT + EN)
        // DB Category: "Salary"
        KEYWORD_TO_CATEGORY.put("salário", "Salary");
        KEYWORD_TO_CATEGORY.put("salario", "Salary");
        KEYWORD_TO_CATEGORY.put("salary", "Salary");
        KEYWORD_TO_CATEGORY.put("ordenado", "Salary");
        KEYWORD_TO_CATEGORY.put("vencimento", "Salary");
        KEYWORD_TO_CATEGORY.put("bónus", "Salary");
        KEYWORD_TO_CATEGORY.put("bonus", "Salary");
        KEYWORD_TO_CATEGORY.put("horas extra", "Salary");
        KEYWORD_TO_CATEGORY.put("overtime", "Salary");
        KEYWORD_TO_CATEGORY.put("comissão", "Salary");
        KEYWORD_TO_CATEGORY.put("comissao", "Salary");
        KEYWORD_TO_CATEGORY.put("commission", "Salary");

        // Entertainment keywords (PT + EN)
        // DB Category: "Entertainment"
        KEYWORD_TO_CATEGORY.put("cinema", "Entertainment");
        KEYWORD_TO_CATEGORY.put("movie", "Entertainment");
        KEYWORD_TO_CATEGORY.put("movies", "Entertainment");
        KEYWORD_TO_CATEGORY.put("filme", "Entertainment");
        KEYWORD_TO_CATEGORY.put("filmes", "Entertainment");
        KEYWORD_TO_CATEGORY.put("netflix", "Entertainment");
        KEYWORD_TO_CATEGORY.put("spotify", "Entertainment");
        KEYWORD_TO_CATEGORY.put("hbo", "Entertainment");
        KEYWORD_TO_CATEGORY.put("disney", "Entertainment");
        KEYWORD_TO_CATEGORY.put("streaming", "Entertainment");
        KEYWORD_TO_CATEGORY.put("jogos", "Entertainment");
        KEYWORD_TO_CATEGORY.put("games", "Entertainment");
        KEYWORD_TO_CATEGORY.put("concerto", "Entertainment");
        KEYWORD_TO_CATEGORY.put("concertos", "Entertainment");
        KEYWORD_TO_CATEGORY.put("concert", "Entertainment");
        KEYWORD_TO_CATEGORY.put("entretenimento", "Entertainment");
        KEYWORD_TO_CATEGORY.put("diversão", "Entertainment");
        KEYWORD_TO_CATEGORY.put("diversao", "Entertainment");
        KEYWORD_TO_CATEGORY.put("subscriptions", "Entertainment");
        KEYWORD_TO_CATEGORY.put("subscrição", "Entertainment");
        KEYWORD_TO_CATEGORY.put("subscricao", "Entertainment");

        // Shopping keywords (PT + EN)
        // DB Category: "Shopping"
        KEYWORD_TO_CATEGORY.put("roupa", "Shopping");
        KEYWORD_TO_CATEGORY.put("roupas", "Shopping");
        KEYWORD_TO_CATEGORY.put("clothes", "Shopping");
        KEYWORD_TO_CATEGORY.put("clothing", "Shopping");
        KEYWORD_TO_CATEGORY.put("loja", "Shopping");
        KEYWORD_TO_CATEGORY.put("lojas", "Shopping");
        KEYWORD_TO_CATEGORY.put("store", "Shopping");
        KEYWORD_TO_CATEGORY.put("shopping", "Shopping");
        KEYWORD_TO_CATEGORY.put("compras", "Shopping");
        KEYWORD_TO_CATEGORY.put("eletrónicos", "Shopping");
        KEYWORD_TO_CATEGORY.put("eletronicos", "Shopping");
        KEYWORD_TO_CATEGORY.put("electronics", "Shopping");
        KEYWORD_TO_CATEGORY.put("presente", "Shopping");
        KEYWORD_TO_CATEGORY.put("presentes", "Shopping");
        KEYWORD_TO_CATEGORY.put("gift", "Shopping");
        KEYWORD_TO_CATEGORY.put("gifts", "Shopping");

        // Bills & Utilities keywords (PT + EN)
        // DB Category: "Bills & Utilities"
        KEYWORD_TO_CATEGORY.put("conta", "Bills & Utilities");
        KEYWORD_TO_CATEGORY.put("contas", "Bills & Utilities");
        KEYWORD_TO_CATEGORY.put("bill", "Bills & Utilities");
        KEYWORD_TO_CATEGORY.put("bills", "Bills & Utilities");
        KEYWORD_TO_CATEGORY.put("luz", "Bills & Utilities");
        KEYWORD_TO_CATEGORY.put("eletricidade", "Bills & Utilities");
        KEYWORD_TO_CATEGORY.put("electricity", "Bills & Utilities");
        KEYWORD_TO_CATEGORY.put("água", "Bills & Utilities");
        KEYWORD_TO_CATEGORY.put("agua", "Bills & Utilities");
        KEYWORD_TO_CATEGORY.put("water", "Bills & Utilities");
        KEYWORD_TO_CATEGORY.put("internet", "Bills & Utilities");
        KEYWORD_TO_CATEGORY.put("telefone", "Bills & Utilities");
        KEYWORD_TO_CATEGORY.put("telemóvel", "Bills & Utilities");
        KEYWORD_TO_CATEGORY.put("telemovel", "Bills & Utilities");
        KEYWORD_TO_CATEGORY.put("phone", "Bills & Utilities");
        KEYWORD_TO_CATEGORY.put("renda", "Bills & Utilities");
        KEYWORD_TO_CATEGORY.put("aluguer", "Bills & Utilities");
        KEYWORD_TO_CATEGORY.put("rent", "Bills & Utilities");
        KEYWORD_TO_CATEGORY.put("hipoteca", "Bills & Utilities");
        KEYWORD_TO_CATEGORY.put("mortgage", "Bills & Utilities");

        // Health keywords (PT + EN)
        // DB Category: "Health"
        KEYWORD_TO_CATEGORY.put("farmácia", "Health");
        KEYWORD_TO_CATEGORY.put("farmacia", "Health");
        KEYWORD_TO_CATEGORY.put("pharmacy", "Health");
        KEYWORD_TO_CATEGORY.put("médico", "Health");
        KEYWORD_TO_CATEGORY.put("medico", "Health");
        KEYWORD_TO_CATEGORY.put("doctor", "Health");
        KEYWORD_TO_CATEGORY.put("hospital", "Health");
        KEYWORD_TO_CATEGORY.put("saúde", "Health");
        KEYWORD_TO_CATEGORY.put("saude", "Health");
        KEYWORD_TO_CATEGORY.put("health", "Health");
        KEYWORD_TO_CATEGORY.put("ginásio", "Health");
        KEYWORD_TO_CATEGORY.put("ginasio", "Health");
        KEYWORD_TO_CATEGORY.put("gym", "Health");
        KEYWORD_TO_CATEGORY.put("seguro", "Health");
        KEYWORD_TO_CATEGORY.put("insurance", "Health");
        KEYWORD_TO_CATEGORY.put("medicamento", "Health");
        KEYWORD_TO_CATEGORY.put("medicamentos", "Health");
        KEYWORD_TO_CATEGORY.put("medicine", "Health");

        // Education keywords (PT + EN)
        // DB Category: "Education"
        KEYWORD_TO_CATEGORY.put("educação", "Education");
        KEYWORD_TO_CATEGORY.put("educacao", "Education");
        KEYWORD_TO_CATEGORY.put("education", "Education");
        KEYWORD_TO_CATEGORY.put("curso", "Education");
        KEYWORD_TO_CATEGORY.put("cursos", "Education");
        KEYWORD_TO_CATEGORY.put("course", "Education");
        KEYWORD_TO_CATEGORY.put("courses", "Education");
        KEYWORD_TO_CATEGORY.put("livro", "Education");
        KEYWORD_TO_CATEGORY.put("livros", "Education");
        KEYWORD_TO_CATEGORY.put("book", "Education");
        KEYWORD_TO_CATEGORY.put("books", "Education");
        KEYWORD_TO_CATEGORY.put("escola", "Education");
        KEYWORD_TO_CATEGORY.put("school", "Education");
        KEYWORD_TO_CATEGORY.put("universidade", "Education");
        KEYWORD_TO_CATEGORY.put("university", "Education");
        KEYWORD_TO_CATEGORY.put("propina", "Education");
        KEYWORD_TO_CATEGORY.put("propinas", "Education");
        KEYWORD_TO_CATEGORY.put("tuition", "Education");

        // Investments keywords (PT + EN)
        // DB Category: "Investments"
        KEYWORD_TO_CATEGORY.put("investimento", "Investments");
        KEYWORD_TO_CATEGORY.put("investimentos", "Investments");
        KEYWORD_TO_CATEGORY.put("investment", "Investments");
        KEYWORD_TO_CATEGORY.put("investments", "Investments");
        KEYWORD_TO_CATEGORY.put("dividendos", "Investments");
        KEYWORD_TO_CATEGORY.put("dividends", "Investments");
        KEYWORD_TO_CATEGORY.put("juros", "Investments");
        KEYWORD_TO_CATEGORY.put("interest", "Investments");
        KEYWORD_TO_CATEGORY.put("ações", "Investments");
        KEYWORD_TO_CATEGORY.put("acoes", "Investments");
        KEYWORD_TO_CATEGORY.put("stocks", "Investments");

        // Freelance keywords (PT + EN)
        // DB Category: "Freelance"
        KEYWORD_TO_CATEGORY.put("freelance", "Freelance");
        KEYWORD_TO_CATEGORY.put("freelancer", "Freelance");
        KEYWORD_TO_CATEGORY.put("consultoria", "Freelance");
        KEYWORD_TO_CATEGORY.put("consulting", "Freelance");
        KEYWORD_TO_CATEGORY.put("projeto", "Freelance");
        KEYWORD_TO_CATEGORY.put("projetos", "Freelance");
        KEYWORD_TO_CATEGORY.put("project", "Freelance");
        KEYWORD_TO_CATEGORY.put("projects", "Freelance");

        // Other Expenses (PT + EN)
        // DB Category: "Other Expenses"
        KEYWORD_TO_CATEGORY.put("outros", "Other Expenses");
        KEYWORD_TO_CATEGORY.put("outras", "Other Expenses");
        KEYWORD_TO_CATEGORY.put("other", "Other Expenses");
        KEYWORD_TO_CATEGORY.put("miscellaneous", "Other Expenses");
        KEYWORD_TO_CATEGORY.put("diversos", "Other Expenses");
        KEYWORD_TO_CATEGORY.put("taxa", "Other Expenses");
        KEYWORD_TO_CATEGORY.put("taxas", "Other Expenses");
        KEYWORD_TO_CATEGORY.put("fees", "Other Expenses");
        KEYWORD_TO_CATEGORY.put("doação", "Other Expenses");
        KEYWORD_TO_CATEGORY.put("doacao", "Other Expenses");
        KEYWORD_TO_CATEGORY.put("donation", "Other Expenses");
        KEYWORD_TO_CATEGORY.put("donations", "Other Expenses");

        // Other Income (PT + EN)
        // DB Category: "Other Income"
        KEYWORD_TO_CATEGORY.put("reembolso", "Other Income");
        KEYWORD_TO_CATEGORY.put("reembolsos", "Other Income");
        KEYWORD_TO_CATEGORY.put("refund", "Other Income");
        KEYWORD_TO_CATEGORY.put("refunds", "Other Income");
        KEYWORD_TO_CATEGORY.put("cashback", "Other Income");

        // Gifts Received (PT + EN)
        // DB Category: "Gifts Received"
        KEYWORD_TO_CATEGORY.put("presente recebido", "Gifts Received");
        KEYWORD_TO_CATEGORY.put("presentes recebidos", "Gifts Received");
        KEYWORD_TO_CATEGORY.put("gift received", "Gifts Received");
        KEYWORD_TO_CATEGORY.put("prenda", "Gifts Received");
        KEYWORD_TO_CATEGORY.put("prendas", "Gifts Received");
    }

    // =====================================================
    // KEYWORD TO SUBCATEGORY MAPPING
    // =====================================================
    // Keywords map to actual subcategory names in the database.

    private static final Map<String, String> KEYWORD_TO_SUBCATEGORY = new HashMap<>();

    static {
        // Food & Dining subcategories (PT + EN)
        // DB Subcategories: Restaurants, Groceries, Fast Food, Coffee, Delivery
        KEYWORD_TO_SUBCATEGORY.put("supermercado", "Groceries");
        KEYWORD_TO_SUBCATEGORY.put("supermercados", "Groceries");
        KEYWORD_TO_SUBCATEGORY.put("supermarket", "Groceries");
        KEYWORD_TO_SUBCATEGORY.put("mercado", "Groceries");
        KEYWORD_TO_SUBCATEGORY.put("mercados", "Groceries");
        KEYWORD_TO_SUBCATEGORY.put("grocery", "Groceries");
        KEYWORD_TO_SUBCATEGORY.put("groceries", "Groceries");
        KEYWORD_TO_SUBCATEGORY.put("mercearia", "Groceries");
        KEYWORD_TO_SUBCATEGORY.put("restaurante", "Restaurants");
        KEYWORD_TO_SUBCATEGORY.put("restaurantes", "Restaurants");
        KEYWORD_TO_SUBCATEGORY.put("restaurant", "Restaurants");
        KEYWORD_TO_SUBCATEGORY.put("restaurants", "Restaurants");
        KEYWORD_TO_SUBCATEGORY.put("almoço", "Restaurants");
        KEYWORD_TO_SUBCATEGORY.put("almoco", "Restaurants");
        KEYWORD_TO_SUBCATEGORY.put("jantar", "Restaurants");
        KEYWORD_TO_SUBCATEGORY.put("jantares", "Restaurants");
        KEYWORD_TO_SUBCATEGORY.put("café", "Coffee");
        KEYWORD_TO_SUBCATEGORY.put("cafés", "Coffee");
        KEYWORD_TO_SUBCATEGORY.put("cafe", "Coffee");
        KEYWORD_TO_SUBCATEGORY.put("cafes", "Coffee");
        KEYWORD_TO_SUBCATEGORY.put("coffee", "Coffee");
        KEYWORD_TO_SUBCATEGORY.put("fast food", "Fast Food");
        KEYWORD_TO_SUBCATEGORY.put("mcdonalds", "Fast Food");
        KEYWORD_TO_SUBCATEGORY.put("mcdonald's", "Fast Food");
        KEYWORD_TO_SUBCATEGORY.put("burger king", "Fast Food");
        KEYWORD_TO_SUBCATEGORY.put("kfc", "Fast Food");
        KEYWORD_TO_SUBCATEGORY.put("delivery", "Delivery");
        KEYWORD_TO_SUBCATEGORY.put("entrega", "Delivery");
        KEYWORD_TO_SUBCATEGORY.put("uber eats", "Delivery");
        KEYWORD_TO_SUBCATEGORY.put("glovo", "Delivery");
        KEYWORD_TO_SUBCATEGORY.put("bolt food", "Delivery");

        // Transportation subcategories (PT + EN)
        // DB Subcategories: Fuel, Public Transport, Taxi/Uber, Parking, Car Maintenance
        KEYWORD_TO_SUBCATEGORY.put("uber", "Taxi/Uber");
        KEYWORD_TO_SUBCATEGORY.put("bolt", "Taxi/Uber");
        KEYWORD_TO_SUBCATEGORY.put("taxi", "Taxi/Uber");
        KEYWORD_TO_SUBCATEGORY.put("taxis", "Taxi/Uber");
        KEYWORD_TO_SUBCATEGORY.put("táxi", "Taxi/Uber");
        KEYWORD_TO_SUBCATEGORY.put("táxis", "Taxi/Uber");
        KEYWORD_TO_SUBCATEGORY.put("gasolina", "Fuel");
        KEYWORD_TO_SUBCATEGORY.put("gasóleo", "Fuel");
        KEYWORD_TO_SUBCATEGORY.put("gasoleo", "Fuel");
        KEYWORD_TO_SUBCATEGORY.put("gas", "Fuel");
        KEYWORD_TO_SUBCATEGORY.put("fuel", "Fuel");
        KEYWORD_TO_SUBCATEGORY.put("combustível", "Fuel");
        KEYWORD_TO_SUBCATEGORY.put("combustivel", "Fuel");
        KEYWORD_TO_SUBCATEGORY.put("metro", "Public Transport");
        KEYWORD_TO_SUBCATEGORY.put("bus", "Public Transport");
        KEYWORD_TO_SUBCATEGORY.put("autocarro", "Public Transport");
        KEYWORD_TO_SUBCATEGORY.put("autocarros", "Public Transport");
        KEYWORD_TO_SUBCATEGORY.put("ônibus", "Public Transport");
        KEYWORD_TO_SUBCATEGORY.put("onibus", "Public Transport");
        KEYWORD_TO_SUBCATEGORY.put("comboio", "Public Transport");
        KEYWORD_TO_SUBCATEGORY.put("train", "Public Transport");
        KEYWORD_TO_SUBCATEGORY.put("transporte público", "Public Transport");
        KEYWORD_TO_SUBCATEGORY.put("transporte publico", "Public Transport");
        KEYWORD_TO_SUBCATEGORY.put("estacionamento", "Parking");
        KEYWORD_TO_SUBCATEGORY.put("parking", "Parking");
        KEYWORD_TO_SUBCATEGORY.put("parque", "Parking");
        KEYWORD_TO_SUBCATEGORY.put("oficina", "Car Maintenance");
        KEYWORD_TO_SUBCATEGORY.put("manutenção", "Car Maintenance");
        KEYWORD_TO_SUBCATEGORY.put("manutencao", "Car Maintenance");
        KEYWORD_TO_SUBCATEGORY.put("car maintenance", "Car Maintenance");

        // Salary subcategories (PT + EN)
        // DB Subcategories: Monthly Salary, Bonus, Overtime, Commission
        KEYWORD_TO_SUBCATEGORY.put("salário", "Monthly Salary");
        KEYWORD_TO_SUBCATEGORY.put("salario", "Monthly Salary");
        KEYWORD_TO_SUBCATEGORY.put("salary", "Monthly Salary");
        KEYWORD_TO_SUBCATEGORY.put("ordenado", "Monthly Salary");
        KEYWORD_TO_SUBCATEGORY.put("vencimento", "Monthly Salary");
        KEYWORD_TO_SUBCATEGORY.put("bónus", "Bonus");
        KEYWORD_TO_SUBCATEGORY.put("bonus", "Bonus");
        KEYWORD_TO_SUBCATEGORY.put("horas extra", "Overtime");
        KEYWORD_TO_SUBCATEGORY.put("overtime", "Overtime");
        KEYWORD_TO_SUBCATEGORY.put("comissão", "Commission");
        KEYWORD_TO_SUBCATEGORY.put("comissao", "Commission");
        KEYWORD_TO_SUBCATEGORY.put("commission", "Commission");

        // Entertainment subcategories (PT + EN)
        // DB Subcategories: Movies, Games, Concerts, Sports, Subscriptions
        KEYWORD_TO_SUBCATEGORY.put("cinema", "Movies");
        KEYWORD_TO_SUBCATEGORY.put("movie", "Movies");
        KEYWORD_TO_SUBCATEGORY.put("movies", "Movies");
        KEYWORD_TO_SUBCATEGORY.put("filme", "Movies");
        KEYWORD_TO_SUBCATEGORY.put("filmes", "Movies");
        KEYWORD_TO_SUBCATEGORY.put("jogos", "Games");
        KEYWORD_TO_SUBCATEGORY.put("jogo", "Games");
        KEYWORD_TO_SUBCATEGORY.put("games", "Games");
        KEYWORD_TO_SUBCATEGORY.put("game", "Games");
        KEYWORD_TO_SUBCATEGORY.put("playstation", "Games");
        KEYWORD_TO_SUBCATEGORY.put("xbox", "Games");
        KEYWORD_TO_SUBCATEGORY.put("nintendo", "Games");
        KEYWORD_TO_SUBCATEGORY.put("concerto", "Concerts");
        KEYWORD_TO_SUBCATEGORY.put("concertos", "Concerts");
        KEYWORD_TO_SUBCATEGORY.put("concert", "Concerts");
        KEYWORD_TO_SUBCATEGORY.put("concerts", "Concerts");
        KEYWORD_TO_SUBCATEGORY.put("festival", "Concerts");
        KEYWORD_TO_SUBCATEGORY.put("desporto", "Sports");
        KEYWORD_TO_SUBCATEGORY.put("sports", "Sports");
        KEYWORD_TO_SUBCATEGORY.put("futebol", "Sports");
        KEYWORD_TO_SUBCATEGORY.put("football", "Sports");
        KEYWORD_TO_SUBCATEGORY.put("netflix", "Subscriptions");
        KEYWORD_TO_SUBCATEGORY.put("spotify", "Subscriptions");
        KEYWORD_TO_SUBCATEGORY.put("hbo", "Subscriptions");
        KEYWORD_TO_SUBCATEGORY.put("disney", "Subscriptions");
        KEYWORD_TO_SUBCATEGORY.put("streaming", "Subscriptions");
        KEYWORD_TO_SUBCATEGORY.put("subscrição", "Subscriptions");
        KEYWORD_TO_SUBCATEGORY.put("subscricao", "Subscriptions");
        KEYWORD_TO_SUBCATEGORY.put("subscription", "Subscriptions");

        // Shopping subcategories (PT + EN)
        // DB Subcategories: Clothing, Electronics, Home & Garden, Personal Care, Gifts
        KEYWORD_TO_SUBCATEGORY.put("roupa", "Clothing");
        KEYWORD_TO_SUBCATEGORY.put("roupas", "Clothing");
        KEYWORD_TO_SUBCATEGORY.put("clothes", "Clothing");
        KEYWORD_TO_SUBCATEGORY.put("clothing", "Clothing");
        KEYWORD_TO_SUBCATEGORY.put("vestuário", "Clothing");
        KEYWORD_TO_SUBCATEGORY.put("vestuario", "Clothing");
        KEYWORD_TO_SUBCATEGORY.put("eletrónicos", "Electronics");
        KEYWORD_TO_SUBCATEGORY.put("eletronicos", "Electronics");
        KEYWORD_TO_SUBCATEGORY.put("electronics", "Electronics");
        KEYWORD_TO_SUBCATEGORY.put("computador", "Electronics");
        KEYWORD_TO_SUBCATEGORY.put("computer", "Electronics");
        KEYWORD_TO_SUBCATEGORY.put("tablet", "Electronics");
        KEYWORD_TO_SUBCATEGORY.put("portátil", "Electronics");
        KEYWORD_TO_SUBCATEGORY.put("portatil", "Electronics");
        KEYWORD_TO_SUBCATEGORY.put("laptop", "Electronics");
        KEYWORD_TO_SUBCATEGORY.put("casa", "Home & Garden");
        KEYWORD_TO_SUBCATEGORY.put("jardim", "Home & Garden");
        KEYWORD_TO_SUBCATEGORY.put("home", "Home & Garden");
        KEYWORD_TO_SUBCATEGORY.put("garden", "Home & Garden");
        KEYWORD_TO_SUBCATEGORY.put("decoração", "Home & Garden");
        KEYWORD_TO_SUBCATEGORY.put("decoracao", "Home & Garden");
        KEYWORD_TO_SUBCATEGORY.put("higiene", "Personal Care");
        KEYWORD_TO_SUBCATEGORY.put("personal care", "Personal Care");
        KEYWORD_TO_SUBCATEGORY.put("cuidado pessoal", "Personal Care");
        KEYWORD_TO_SUBCATEGORY.put("presente", "Gifts");
        KEYWORD_TO_SUBCATEGORY.put("presentes", "Gifts");
        KEYWORD_TO_SUBCATEGORY.put("gift", "Gifts");
        KEYWORD_TO_SUBCATEGORY.put("gifts", "Gifts");
        KEYWORD_TO_SUBCATEGORY.put("prenda", "Gifts");
        KEYWORD_TO_SUBCATEGORY.put("prendas", "Gifts");

        // Bills & Utilities subcategories (PT + EN)
        // DB Subcategories: Electricity, Water, Internet, Phone, Rent/Mortgage
        KEYWORD_TO_SUBCATEGORY.put("luz", "Electricity");
        KEYWORD_TO_SUBCATEGORY.put("eletricidade", "Electricity");
        KEYWORD_TO_SUBCATEGORY.put("electricity", "Electricity");
        KEYWORD_TO_SUBCATEGORY.put("água", "Water");
        KEYWORD_TO_SUBCATEGORY.put("agua", "Water");
        KEYWORD_TO_SUBCATEGORY.put("water", "Water");
        KEYWORD_TO_SUBCATEGORY.put("internet", "Internet");
        KEYWORD_TO_SUBCATEGORY.put("wifi", "Internet");
        KEYWORD_TO_SUBCATEGORY.put("telefone", "Phone");
        KEYWORD_TO_SUBCATEGORY.put("phone", "Phone");
        KEYWORD_TO_SUBCATEGORY.put("telemóvel", "Phone");
        KEYWORD_TO_SUBCATEGORY.put("telemovel", "Phone");
        KEYWORD_TO_SUBCATEGORY.put("mobile", "Phone");
        KEYWORD_TO_SUBCATEGORY.put("renda", "Rent/Mortgage");
        KEYWORD_TO_SUBCATEGORY.put("aluguer", "Rent/Mortgage");
        KEYWORD_TO_SUBCATEGORY.put("rent", "Rent/Mortgage");
        KEYWORD_TO_SUBCATEGORY.put("hipoteca", "Rent/Mortgage");
        KEYWORD_TO_SUBCATEGORY.put("mortgage", "Rent/Mortgage");

        // Health subcategories (PT + EN)
        // DB Subcategories: Medical, Pharmacy, Gym, Insurance
        KEYWORD_TO_SUBCATEGORY.put("médico", "Medical");
        KEYWORD_TO_SUBCATEGORY.put("medico", "Medical");
        KEYWORD_TO_SUBCATEGORY.put("doctor", "Medical");
        KEYWORD_TO_SUBCATEGORY.put("hospital", "Medical");
        KEYWORD_TO_SUBCATEGORY.put("consulta", "Medical");
        KEYWORD_TO_SUBCATEGORY.put("medical", "Medical");
        KEYWORD_TO_SUBCATEGORY.put("farmácia", "Pharmacy");
        KEYWORD_TO_SUBCATEGORY.put("farmacia", "Pharmacy");
        KEYWORD_TO_SUBCATEGORY.put("pharmacy", "Pharmacy");
        KEYWORD_TO_SUBCATEGORY.put("medicamento", "Pharmacy");
        KEYWORD_TO_SUBCATEGORY.put("medicamentos", "Pharmacy");
        KEYWORD_TO_SUBCATEGORY.put("medicine", "Pharmacy");
        KEYWORD_TO_SUBCATEGORY.put("ginásio", "Gym");
        KEYWORD_TO_SUBCATEGORY.put("ginasio", "Gym");
        KEYWORD_TO_SUBCATEGORY.put("gym", "Gym");
        KEYWORD_TO_SUBCATEGORY.put("fitness", "Gym");
        KEYWORD_TO_SUBCATEGORY.put("seguro", "Insurance");
        KEYWORD_TO_SUBCATEGORY.put("insurance", "Insurance");
        KEYWORD_TO_SUBCATEGORY.put("seguro saúde", "Insurance");
        KEYWORD_TO_SUBCATEGORY.put("seguro saude", "Insurance");

        // Education subcategories (PT + EN)
        // DB Subcategories: Courses, Books, School Supplies, Tuition
        KEYWORD_TO_SUBCATEGORY.put("curso", "Courses");
        KEYWORD_TO_SUBCATEGORY.put("cursos", "Courses");
        KEYWORD_TO_SUBCATEGORY.put("course", "Courses");
        KEYWORD_TO_SUBCATEGORY.put("courses", "Courses");
        KEYWORD_TO_SUBCATEGORY.put("formação", "Courses");
        KEYWORD_TO_SUBCATEGORY.put("formacao", "Courses");
        KEYWORD_TO_SUBCATEGORY.put("livro", "Books");
        KEYWORD_TO_SUBCATEGORY.put("livros", "Books");
        KEYWORD_TO_SUBCATEGORY.put("book", "Books");
        KEYWORD_TO_SUBCATEGORY.put("books", "Books");
        KEYWORD_TO_SUBCATEGORY.put("material escolar", "School Supplies");
        KEYWORD_TO_SUBCATEGORY.put("school supplies", "School Supplies");
        KEYWORD_TO_SUBCATEGORY.put("propina", "Tuition");
        KEYWORD_TO_SUBCATEGORY.put("propinas", "Tuition");
        KEYWORD_TO_SUBCATEGORY.put("tuition", "Tuition");

        // Investments subcategories (PT + EN)
        // DB Subcategories: Dividends, Interest, Capital Gains, Rental Income
        KEYWORD_TO_SUBCATEGORY.put("dividendos", "Dividends");
        KEYWORD_TO_SUBCATEGORY.put("dividends", "Dividends");
        KEYWORD_TO_SUBCATEGORY.put("juros", "Interest");
        KEYWORD_TO_SUBCATEGORY.put("interest", "Interest");
        KEYWORD_TO_SUBCATEGORY.put("mais-valias", "Capital Gains");
        KEYWORD_TO_SUBCATEGORY.put("capital gains", "Capital Gains");
        KEYWORD_TO_SUBCATEGORY.put("arrendamento", "Rental Income");
        KEYWORD_TO_SUBCATEGORY.put("rental income", "Rental Income");

        // Freelance subcategories (PT + EN)
        // DB Subcategories: Consulting, Projects, Gigs
        KEYWORD_TO_SUBCATEGORY.put("consultoria", "Consulting");
        KEYWORD_TO_SUBCATEGORY.put("consulting", "Consulting");
        KEYWORD_TO_SUBCATEGORY.put("projeto", "Projects");
        KEYWORD_TO_SUBCATEGORY.put("projetos", "Projects");
        KEYWORD_TO_SUBCATEGORY.put("project", "Projects");
        KEYWORD_TO_SUBCATEGORY.put("projects", "Projects");
        KEYWORD_TO_SUBCATEGORY.put("trabalho", "Gigs");
        KEYWORD_TO_SUBCATEGORY.put("gig", "Gigs");
        KEYWORD_TO_SUBCATEGORY.put("gigs", "Gigs");

        // Other Income subcategories (PT + EN)
        // DB Subcategories: Refunds, Cashback, Reimbursements, Miscellaneous
        KEYWORD_TO_SUBCATEGORY.put("reembolso", "Refunds");
        KEYWORD_TO_SUBCATEGORY.put("reembolsos", "Refunds");
        KEYWORD_TO_SUBCATEGORY.put("refund", "Refunds");
        KEYWORD_TO_SUBCATEGORY.put("refunds", "Refunds");
        KEYWORD_TO_SUBCATEGORY.put("cashback", "Cashback");
        KEYWORD_TO_SUBCATEGORY.put("devolução", "Reimbursements");
        KEYWORD_TO_SUBCATEGORY.put("devolucao", "Reimbursements");
        KEYWORD_TO_SUBCATEGORY.put("reimbursement", "Reimbursements");

        // Other Expenses subcategories (PT + EN)
        // DB Subcategories: Miscellaneous, Fees, Donations
        KEYWORD_TO_SUBCATEGORY.put("diversos", "Miscellaneous");
        KEYWORD_TO_SUBCATEGORY.put("miscellaneous", "Miscellaneous");
        KEYWORD_TO_SUBCATEGORY.put("taxa", "Fees");
        KEYWORD_TO_SUBCATEGORY.put("taxas", "Fees");
        KEYWORD_TO_SUBCATEGORY.put("fees", "Fees");
        KEYWORD_TO_SUBCATEGORY.put("comissão bancária", "Fees");
        KEYWORD_TO_SUBCATEGORY.put("doação", "Donations");
        KEYWORD_TO_SUBCATEGORY.put("doacao", "Donations");
        KEYWORD_TO_SUBCATEGORY.put("donation", "Donations");
        KEYWORD_TO_SUBCATEGORY.put("donations", "Donations");
        KEYWORD_TO_SUBCATEGORY.put("caridade", "Donations");
    }

    public VoiceClassificationService(
            WalletService walletService,
            RuleBasedTransactionParser parser,
            CategoryRepository categoryRepository,
            SubcategoryRepository subcategoryRepository) {
        this.walletService = walletService;
        this.parser = parser;
        this.categoryRepository = categoryRepository;
        this.subcategoryRepository = subcategoryRepository;
    }

    /**
     * Classifies text input and returns a draft transaction.
     *
     * IMPORTANT: This method NEVER saves anything to the database.
     * It only reads categories/subcategories to validate matches.
     *
     * @param request The voice request containing walletId and text
     * @param user The authenticated user
     * @return TransactionDraftDto with all detected/classified fields
     */
    @Transactional(readOnly = true) // Read-only transaction - no DB writes
    public TransactionDraftDto classifyText(VoiceRequestDto request, User user) {
        // Step 1: Validate wallet ownership
        Wallet wallet = walletService.getWalletEntityByIdAndUser(request.getWalletId(), user);

        // Step 2: Normalize input text
        String text = request.getText().toLowerCase().trim();
        String originalText = request.getText(); // Keep original for description

        // Step 3: Detect transaction type using rule-based parser
        TransactionType type = parser.detectTransactionType(text);

        // Step 4: Extract amount using rule-based parser
        RuleBasedTransactionParser.AmountResult amountResult = parser.extractAmount(text);
        BigDecimal amount = amountResult.amount;
        boolean amountDetected = amountResult.detected;

        // Validate amount - if not detected, throw error
        if (!amountDetected || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Could not extract a valid amount from the text. Please include a number like '23.50' or '100 euros'.");
        }

        // Step 5: Extract date using rule-based parser
        RuleBasedTransactionParser.DateResult dateResult = parser.extractDate(text);
        LocalDate date = dateResult.date;
        boolean dateDetected = dateResult.explicitlyDetected;

        // Step 6: Detect and match category against database
        CategoryMatchResult categoryMatch = detectAndMatchCategory(text, type, user.getId());

        // Step 7: Detect and match subcategory against database
        String subcategory = detectAndMatchSubcategory(text, categoryMatch.categoryName, categoryMatch.categoryId, user.getId());

        // Step 8: Build and return the draft DTO
        TransactionDraftDto draft = new TransactionDraftDto();
        draft.setWalletId(wallet.getId());
        draft.setType(type);
        draft.setAmount(amount);
        draft.setCategory(categoryMatch.categoryName);
        draft.setSubcategory(subcategory);
        draft.setDate(date);
        draft.setDescription(originalText); // Use original text as description

        // Set confidence indicators
        draft.setAmountDetected(amountDetected);
        draft.setCategoryMatched(categoryMatch.matchedInDb);
        draft.setDateDetected(dateDetected);

        return draft;
    }

    /**
     * Helper class to hold category match results.
     */
    private static class CategoryMatchResult {
        String categoryName;
        Long categoryId;
        boolean matchedInDb;

        CategoryMatchResult(String categoryName, Long categoryId, boolean matchedInDb) {
            this.categoryName = categoryName;
            this.categoryId = categoryId;
            this.matchedInDb = matchedInDb;
        }
    }

    /**
     * Detects category from text keywords and matches against database.
     *
     * Logic:
     * 1. Search text for known keywords
     * 2. Get the suggested category name from keyword mapping
     * 3. Try to find that category in the database (default + user's categories)
     * 4. If found, use it; if not, default to "Other"
     *
     * @param text The input text (lowercase)
     * @param type The detected transaction type
     * @param userId The user's ID
     * @return CategoryMatchResult with name, ID, and whether it matched in DB
     */
    private CategoryMatchResult detectAndMatchCategory(String text, TransactionType type, Long userId) {
        // Step 1: Find a keyword match in the text
        String suggestedCategoryName = null;
        for (Map.Entry<String, String> entry : KEYWORD_TO_CATEGORY.entrySet()) {
            if (text.contains(entry.getKey())) {
                suggestedCategoryName = entry.getValue();
                break;
            }
        }

        // Step 2: If we found a keyword, try to match it in the database
        if (suggestedCategoryName != null) {
            // Look for the category in DB (case-insensitive search would be better, but keeping it simple)
            Optional<Category> categoryOpt = categoryRepository.findByNameForUser(suggestedCategoryName, userId);

            if (categoryOpt.isPresent()) {
                Category category = categoryOpt.get();
                // Verify the category type matches the transaction type
                if (category.getType() == type) {
                    return new CategoryMatchResult(category.getName(), category.getId(), true);
                }
            }

            // Category name was suggested but not found in DB for this type
            // Try to find any category with this name (might be different type)
            // If still not found, we'll default to "Other"
        }

        // Step 3: No keyword match or DB match - default to "Other"
        // Try to find "Other" category in DB for this transaction type
        Optional<Category> otherCategory = categoryRepository.findByNameForUser("Other", userId);
        if (otherCategory.isPresent() && otherCategory.get().getType() == type) {
            return new CategoryMatchResult("Other", otherCategory.get().getId(), true);
        }

        // If no "Other" category exists, just return "Other" as string (not matched in DB)
        return new CategoryMatchResult("Other", null, false);
    }

    /**
     * Detects subcategory from text keywords and matches against database.
     *
     * Logic:
     * 1. Search text for known keywords
     * 2. Get the suggested subcategory name from keyword mapping
     * 3. If we have a category ID, try to find the subcategory in DB
     * 4. If found, use it; if not, default to "General"
     *
     * @param text The input text (lowercase)
     * @param categoryName The detected category name
     * @param categoryId The detected category ID (can be null)
     * @param userId The user's ID
     * @return The matched or default subcategory name
     */
    private String detectAndMatchSubcategory(String text, String categoryName, Long categoryId, Long userId) {
        // Step 1: Find a keyword match in the text
        String suggestedSubcategoryName = null;
        for (Map.Entry<String, String> entry : KEYWORD_TO_SUBCATEGORY.entrySet()) {
            if (text.contains(entry.getKey())) {
                suggestedSubcategoryName = entry.getValue();
                break;
            }
        }

        // Step 2: If we have a category ID and a suggested subcategory, try to match in DB
        if (categoryId != null && suggestedSubcategoryName != null) {
            Optional<Subcategory> subcategoryOpt = subcategoryRepository
                    .findByNameForCategoryAndUser(suggestedSubcategoryName, categoryId, userId);

            if (subcategoryOpt.isPresent()) {
                return subcategoryOpt.get().getName();
            }
        }

        // Step 3: Try to find "General" subcategory if we have a category
        if (categoryId != null) {
            Optional<Subcategory> generalSubcat = subcategoryRepository
                    .findByNameForCategoryAndUser("General", categoryId, userId);

            if (generalSubcat.isPresent()) {
                return "General";
            }

            // If no "General", get the first available subcategory for this category
            List<Subcategory> availableSubcats = subcategoryRepository
                    .findAllAvailableForUserByCategory(categoryId, userId);

            if (!availableSubcats.isEmpty()) {
                return availableSubcats.get(0).getName();
            }
        }

        // Default to "General" even if not in DB (transaction creation will handle validation)
        return suggestedSubcategoryName != null ? suggestedSubcategoryName : "General";
    }
}

