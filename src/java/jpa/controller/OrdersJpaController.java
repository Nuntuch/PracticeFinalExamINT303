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
import jpa.model.Orders;

/**
 *
 * @author Nuntuch Thongyoo
 */
public class OrdersJpaController implements Serializable {

    public OrdersJpaController(EntityManagerFactory emf) {
        this.emf = emf;
    }
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(Orders orders) throws PreexistingEntityException, Exception {
        if (orders.getOrderItemList() == null) {
            orders.setOrderItemList(new ArrayList<OrderItem>());
        }
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            List<OrderItem> attachedOrderItemList = new ArrayList<OrderItem>();
            for (OrderItem orderItemListOrderItemToAttach : orders.getOrderItemList()) {
                orderItemListOrderItemToAttach = em.getReference(orderItemListOrderItemToAttach.getClass(), orderItemListOrderItemToAttach.getOrderItemPK());
                attachedOrderItemList.add(orderItemListOrderItemToAttach);
            }
            orders.setOrderItemList(attachedOrderItemList);
            em.persist(orders);
            for (OrderItem orderItemListOrderItem : orders.getOrderItemList()) {
                Orders oldOrdersOfOrderItemListOrderItem = orderItemListOrderItem.getOrders();
                orderItemListOrderItem.setOrders(orders);
                orderItemListOrderItem = em.merge(orderItemListOrderItem);
                if (oldOrdersOfOrderItemListOrderItem != null) {
                    oldOrdersOfOrderItemListOrderItem.getOrderItemList().remove(orderItemListOrderItem);
                    oldOrdersOfOrderItemListOrderItem = em.merge(oldOrdersOfOrderItemListOrderItem);
                }
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            if (findOrders(orders.getOrderId()) != null) {
                throw new PreexistingEntityException("Orders " + orders + " already exists.", ex);
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(Orders orders) throws IllegalOrphanException, NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Orders persistentOrders = em.find(Orders.class, orders.getOrderId());
            List<OrderItem> orderItemListOld = persistentOrders.getOrderItemList();
            List<OrderItem> orderItemListNew = orders.getOrderItemList();
            List<String> illegalOrphanMessages = null;
            for (OrderItem orderItemListOldOrderItem : orderItemListOld) {
                if (!orderItemListNew.contains(orderItemListOldOrderItem)) {
                    if (illegalOrphanMessages == null) {
                        illegalOrphanMessages = new ArrayList<String>();
                    }
                    illegalOrphanMessages.add("You must retain OrderItem " + orderItemListOldOrderItem + " since its orders field is not nullable.");
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
            orders.setOrderItemList(orderItemListNew);
            orders = em.merge(orders);
            for (OrderItem orderItemListNewOrderItem : orderItemListNew) {
                if (!orderItemListOld.contains(orderItemListNewOrderItem)) {
                    Orders oldOrdersOfOrderItemListNewOrderItem = orderItemListNewOrderItem.getOrders();
                    orderItemListNewOrderItem.setOrders(orders);
                    orderItemListNewOrderItem = em.merge(orderItemListNewOrderItem);
                    if (oldOrdersOfOrderItemListNewOrderItem != null && !oldOrdersOfOrderItemListNewOrderItem.equals(orders)) {
                        oldOrdersOfOrderItemListNewOrderItem.getOrderItemList().remove(orderItemListNewOrderItem);
                        oldOrdersOfOrderItemListNewOrderItem = em.merge(oldOrdersOfOrderItemListNewOrderItem);
                    }
                }
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Integer id = orders.getOrderId();
                if (findOrders(id) == null) {
                    throw new NonexistentEntityException("The orders with id " + id + " no longer exists.");
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
            Orders orders;
            try {
                orders = em.getReference(Orders.class, id);
                orders.getOrderId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The orders with id " + id + " no longer exists.", enfe);
            }
            List<String> illegalOrphanMessages = null;
            List<OrderItem> orderItemListOrphanCheck = orders.getOrderItemList();
            for (OrderItem orderItemListOrphanCheckOrderItem : orderItemListOrphanCheck) {
                if (illegalOrphanMessages == null) {
                    illegalOrphanMessages = new ArrayList<String>();
                }
                illegalOrphanMessages.add("This Orders (" + orders + ") cannot be destroyed since the OrderItem " + orderItemListOrphanCheckOrderItem + " in its orderItemList field has a non-nullable orders field.");
            }
            if (illegalOrphanMessages != null) {
                throw new IllegalOrphanException(illegalOrphanMessages);
            }
            em.remove(orders);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Orders> findOrdersEntities() {
        return findOrdersEntities(true, -1, -1);
    }

    public List<Orders> findOrdersEntities(int maxResults, int firstResult) {
        return findOrdersEntities(false, maxResults, firstResult);
    }

    private List<Orders> findOrdersEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Orders.class));
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

    public Orders findOrders(Integer id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Orders.class, id);
        } finally {
            em.close();
        }
    }

    public int getOrdersCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Orders> rt = cq.from(Orders.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
}
