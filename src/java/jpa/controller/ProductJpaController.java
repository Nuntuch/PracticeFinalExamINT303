/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jpa.controller;

import java.io.Serializable;
import javax.persistence.Query;
import javax.persistence.EntityNotFoundException;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import jpa.model.OrderItem;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import jpa.controller.exceptions.IllegalOrphanException;
import jpa.controller.exceptions.NonexistentEntityException;
import jpa.controller.exceptions.PreexistingEntityException;
import jpa.model.Product;

/**
 *
 * @author Nuntuch Thongyoo
 */
public class ProductJpaController implements Serializable {

    public ProductJpaController(EntityManagerFactory emf) {
        this.emf = emf;
    }
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(Product product) throws PreexistingEntityException, Exception {
        if (product.getOrderItemList() == null) {
            product.setOrderItemList(new ArrayList<OrderItem>());
        }
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            List<OrderItem> attachedOrderItemList = new ArrayList<OrderItem>();
            for (OrderItem orderItemListOrderItemToAttach : product.getOrderItemList()) {
                orderItemListOrderItemToAttach = em.getReference(orderItemListOrderItemToAttach.getClass(), orderItemListOrderItemToAttach.getOrderItemPK());
                attachedOrderItemList.add(orderItemListOrderItemToAttach);
            }
            product.setOrderItemList(attachedOrderItemList);
            em.persist(product);
            for (OrderItem orderItemListOrderItem : product.getOrderItemList()) {
                Product oldProductOfOrderItemListOrderItem = orderItemListOrderItem.getProduct();
                orderItemListOrderItem.setProduct(product);
                orderItemListOrderItem = em.merge(orderItemListOrderItem);
                if (oldProductOfOrderItemListOrderItem != null) {
                    oldProductOfOrderItemListOrderItem.getOrderItemList().remove(orderItemListOrderItem);
                    oldProductOfOrderItemListOrderItem = em.merge(oldProductOfOrderItemListOrderItem);
                }
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            if (findProduct(product.getProductId()) != null) {
                throw new PreexistingEntityException("Product " + product + " already exists.", ex);
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(Product product) throws IllegalOrphanException, NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Product persistentProduct = em.find(Product.class, product.getProductId());
            List<OrderItem> orderItemListOld = persistentProduct.getOrderItemList();
            List<OrderItem> orderItemListNew = product.getOrderItemList();
            List<String> illegalOrphanMessages = null;
            for (OrderItem orderItemListOldOrderItem : orderItemListOld) {
                if (!orderItemListNew.contains(orderItemListOldOrderItem)) {
                    if (illegalOrphanMessages == null) {
                        illegalOrphanMessages = new ArrayList<String>();
                    }
                    illegalOrphanMessages.add("You must retain OrderItem " + orderItemListOldOrderItem + " since its product field is not nullable.");
                }
            }
            if (illegalOrphanMessages != null) {
                throw new IllegalOrphanException(illegalOrphanMessages);
            }
            List<OrderItem> attachedOrderItemListNew = new ArrayList<OrderItem>();
            for (OrderItem orderItemListNewOrderItemToAttach : orderItemListNew) {
                orderItemListNewOrderItemToAttach = em.getReference(orderItemListNewOrderItemToAttach.getClass(), orderItemListNewOrderItemToAttach.getOrderItemPK());
                attachedOrderItemListNew.add(orderItemListNewOrderItemToAttach);
            }
            orderItemListNew = attachedOrderItemListNew;
            product.setOrderItemList(orderItemListNew);
            product = em.merge(product);
            for (OrderItem orderItemListNewOrderItem : orderItemListNew) {
                if (!orderItemListOld.contains(orderItemListNewOrderItem)) {
                    Product oldProductOfOrderItemListNewOrderItem = orderItemListNewOrderItem.getProduct();
                    orderItemListNewOrderItem.setProduct(product);
                    orderItemListNewOrderItem = em.merge(orderItemListNewOrderItem);
                    if (oldProductOfOrderItemListNewOrderItem != null && !oldProductOfOrderItemListNewOrderItem.equals(product)) {
                        oldProductOfOrderItemListNewOrderItem.getOrderItemList().remove(orderItemListNewOrderItem);
                        oldProductOfOrderItemListNewOrderItem = em.merge(oldProductOfOrderItemListNewOrderItem);
                    }
                }
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Integer id = product.getProductId();
                if (findProduct(id) == null) {
                    throw new NonexistentEntityException("The product with id " + id + " no longer exists.");
                }
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void destroy(Integer id) throws IllegalOrphanException, NonexistentEntityException {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Product product;
            try {
                product = em.getReference(Product.class, id);
                product.getProductId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The product with id " + id + " no longer exists.", enfe);
            }
            List<String> illegalOrphanMessages = null;
            List<OrderItem> orderItemListOrphanCheck = product.getOrderItemList();
            for (OrderItem orderItemListOrphanCheckOrderItem : orderItemListOrphanCheck) {
                if (illegalOrphanMessages == null) {
                    illegalOrphanMessages = new ArrayList<String>();
                }
                illegalOrphanMessages.add("This Product (" + product + ") cannot be destroyed since the OrderItem " + orderItemListOrphanCheckOrderItem + " in its orderItemList field has a non-nullable product field.");
            }
            if (illegalOrphanMessages != null) {
                throw new IllegalOrphanException(illegalOrphanMessages);
            }
            em.remove(product);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Product> findProductEntities() {
        return findProductEntities(true, -1, -1);
    }

    public List<Product> findProductEntities(int maxResults, int firstResult) {
        return findProductEntities(false, maxResults, firstResult);
    }

    private List<Product> findProductEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Product.class));
            Query q = em.createQuery(cq);
            if (!all) {
                q.setMaxResults(maxResults);
                q.setFirstResult(firstResult);
            }
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    public Product findProductByName(String name) {

        Product product = new Product();
        EntityManager em = getEntityManager();

        Query q = em.createNamedQuery("Product.findByProductName");
        product = (Product) q.setParameter("name", name);

        return product;
    }

    public Product findProduct(Integer id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Product.class, id);
        } finally {
            em.close();
        }
    }

    public int getProductCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Product> rt = cq.from(Product.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }

}
