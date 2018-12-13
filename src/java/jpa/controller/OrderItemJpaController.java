/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jpa.controller;

import java.io.Serializable;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.persistence.EntityNotFoundException;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import jpa.controller.exceptions.NonexistentEntityException;
import jpa.controller.exceptions.PreexistingEntityException;
import jpa.model.OrderItem;
import jpa.model.OrderItemPK;
import jpa.model.Orders;
import jpa.model.Product;

/**
 *
 * @author Nuntuch Thongyoo
 */
public class OrderItemJpaController implements Serializable {

    public OrderItemJpaController(EntityManagerFactory emf) {
        this.emf = emf;
    }
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(OrderItem orderItem) throws PreexistingEntityException, Exception {
        if (orderItem.getOrderItemPK() == null) {
            orderItem.setOrderItemPK(new OrderItemPK());
        }
        orderItem.getOrderItemPK().setOrderId(orderItem.getOrders().getOrderId());
        orderItem.getOrderItemPK().setProductId(orderItem.getProduct().getProductId());
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Orders orders = orderItem.getOrders();
            if (orders != null) {
                orders = em.getReference(orders.getClass(), orders.getOrderId());
                orderItem.setOrders(orders);
            }
            Product product = orderItem.getProduct();
            if (product != null) {
                product = em.getReference(product.getClass(), product.getProductId());
                orderItem.setProduct(product);
            }
            em.persist(orderItem);
            if (orders != null) {
                orders.getOrderItemList().add(orderItem);
                orders = em.merge(orders);
            }
            if (product != null) {
                product.getOrderItemList().add(orderItem);
                product = em.merge(product);
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            if (findOrderItem(orderItem.getOrderItemPK()) != null) {
                throw new PreexistingEntityException("OrderItem " + orderItem + " already exists.", ex);
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(OrderItem orderItem) throws NonexistentEntityException, Exception {
        orderItem.getOrderItemPK().setOrderId(orderItem.getOrders().getOrderId());
        orderItem.getOrderItemPK().setProductId(orderItem.getProduct().getProductId());
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            OrderItem persistentOrderItem = em.find(OrderItem.class, orderItem.getOrderItemPK());
            Orders ordersOld = persistentOrderItem.getOrders();
            Orders ordersNew = orderItem.getOrders();
            Product productOld = persistentOrderItem.getProduct();
            Product productNew = orderItem.getProduct();
            if (ordersNew != null) {
                ordersNew = em.getReference(ordersNew.getClass(), ordersNew.getOrderId());
                orderItem.setOrders(ordersNew);
            }
            if (productNew != null) {
                productNew = em.getReference(productNew.getClass(), productNew.getProductId());
                orderItem.setProduct(productNew);
            }
            orderItem = em.merge(orderItem);
            if (ordersOld != null && !ordersOld.equals(ordersNew)) {
                ordersOld.getOrderItemList().remove(orderItem);
                ordersOld = em.merge(ordersOld);
            }
            if (ordersNew != null && !ordersNew.equals(ordersOld)) {
                ordersNew.getOrderItemList().add(orderItem);
                ordersNew = em.merge(ordersNew);
            }
            if (productOld != null && !productOld.equals(productNew)) {
                productOld.getOrderItemList().remove(orderItem);
                productOld = em.merge(productOld);
            }
            if (productNew != null && !productNew.equals(productOld)) {
                productNew.getOrderItemList().add(orderItem);
                productNew = em.merge(productNew);
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                OrderItemPK id = orderItem.getOrderItemPK();
                if (findOrderItem(id) == null) {
                    throw new NonexistentEntityException("The orderItem with id " + id + " no longer exists.");
                }
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void destroy(OrderItemPK id) throws NonexistentEntityException {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            OrderItem orderItem;
            try {
                orderItem = em.getReference(OrderItem.class, id);
                orderItem.getOrderItemPK();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The orderItem with id " + id + " no longer exists.", enfe);
            }
            Orders orders = orderItem.getOrders();
            if (orders != null) {
                orders.getOrderItemList().remove(orderItem);
                orders = em.merge(orders);
            }
            Product product = orderItem.getProduct();
            if (product != null) {
                product.getOrderItemList().remove(orderItem);
                product = em.merge(product);
            }
            em.remove(orderItem);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<OrderItem> findOrderItemEntities() {
        return findOrderItemEntities(true, -1, -1);
    }

    public List<OrderItem> findOrderItemEntities(int maxResults, int firstResult) {
        return findOrderItemEntities(false, maxResults, firstResult);
    }

    private List<OrderItem> findOrderItemEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(OrderItem.class));
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

    public OrderItem findOrderItem(OrderItemPK id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(OrderItem.class, id);
        } finally {
            em.close();
        }
    }

    public int getOrderItemCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<OrderItem> rt = cq.from(OrderItem.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
}
