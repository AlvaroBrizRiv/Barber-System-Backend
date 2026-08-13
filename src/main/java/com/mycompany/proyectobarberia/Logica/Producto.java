package com.mycompany.proyectobarberia.Logica;

import com.mycompany.proyectobarberia.Logica.enums.CategoriaProducto;

/**
 * Producto de aseo masculino disponible en la tienda de una barbería.
 *
 * CORRECCIONES respecto a la versión anterior:
 *   - precio:   cambiado de int → double (para valores con decimales).
 *   - "producto": renombrado a "nombre" (nombre más semántico y estándar).
 *   - cantidad: renombrado a "stock" (término correcto en inventario).
 *   - Agregado: barberiaId — cada barbería gestiona su propio catálogo.
 *   - Agregado: descripcion — descripción detallada del producto.
 *   - Agregado: categoria  — enum CategoriaProducto para filtros y búsqueda.
 *   - Agregado: imagenUrl  — URL de la imagen del producto.
 *   - Agregado: activo     — para desactivar sin eliminar.
 */
public class Producto {

    private int id;
    private int barberiaId;             // Multi-tenant: catálogo por barbería
    private String nombre;
    private String descripcion;
    private double precio;             // Precio en CLP, con decimales
    private int stock;                 // Unidades disponibles en inventario
    private CategoriaProducto categoria;
    private String imagenUrl;
    private boolean activo;

    // ─── Constructores ────────────────────────────────────────────────

    public Producto() {
        this.activo = true;
        this.stock = 0;
    }

    /**
     * Constructor para agregar un nuevo producto al catálogo.
     * 
     * @param barberiaId ID de la barbería dueña del producto
     * @param nombre Nombre del producto
     * @param descripcion Descripción general
     * @param precio Precio de venta en moneda local
     * @param stock Cantidad disponible en inventario
     * @param categoria Categoría del producto para filtrado
     * @param imagenUrl URL de la imagen del producto
     */
    public Producto(int barberiaId, String nombre, String descripcion,
                    double precio, int stock, CategoriaProducto categoria,
                    String imagenUrl) {
        this();
        this.barberiaId = barberiaId;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.stock = stock;
        this.categoria = categoria;
        this.imagenUrl = imagenUrl;
    }

    /**
     * Constructor completo para carga desde base de datos.
     * 
     * @param id ID único del producto
     * @param barberiaId ID de la barbería
     * @param nombre Nombre del producto
     * @param descripcion Descripción
     * @param precio Precio en moneda local
     * @param stock Cantidad de inventario
     * @param categoria Categoría asignada
     * @param imagenUrl URL de la foto del producto
     * @param activo Estado actual del producto
     */
    public Producto(int id, int barberiaId, String nombre, String descripcion,
                    double precio, int stock, CategoriaProducto categoria,
                    String imagenUrl, boolean activo) {
        this.id = id;
        this.barberiaId = barberiaId;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.stock = stock;
        this.categoria = categoria;
        this.imagenUrl = imagenUrl;
        this.activo = activo;
    }

    // ─── Métodos de negocio ───────────────────────────────────────────

    /**
     * Verifica si hay stock disponible para vender y está activo.
     * 
     * @return true si está activo y el stock es mayor a 0, false en caso contrario
     */
    public boolean estaDisponible() {
        return this.activo && this.stock > 0;
    }

    /**
     * Reduce el stock en 1 unidad tras la venta de un producto.
     * 
     * @throws IllegalStateException si no hay stock suficiente para realizar la venta
     */
    public void vender() {
        if (this.stock <= 0) {
            throw new IllegalStateException("Sin stock disponible para: " + this.nombre);
        }
        this.stock--;
    }

    // ─── Getters y Setters ────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getBarberiaId() { return barberiaId; }
    public void setBarberiaId(int barberiaId) { this.barberiaId = barberiaId; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public double getPrecio() { return precio; }
    public void setPrecio(double precio) { this.precio = precio; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public CategoriaProducto getCategoria() { return categoria; }
    public void setCategoria(CategoriaProducto categoria) { this.categoria = categoria; }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
