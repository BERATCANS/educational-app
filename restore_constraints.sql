--
-- PostgreSQL database dump
--

\restrict MyqRY5HZek6ke287Fhgcsm1pl4dwXjPExbSxJVxdNIgoHu56Aj5hz0hCJGj7wuj

-- Dumped from database version 16.11
-- Dumped by pg_dump version 16.10 (Debian 16.10-1.pgdg13+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

--
-- Name: book_codes book_codes_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.book_codes
    ADD CONSTRAINT book_codes_code_key UNIQUE (code);


--
-- Name: book_codes book_codes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.book_codes
    ADD CONSTRAINT book_codes_pkey PRIMARY KEY (id);


--
-- Name: book_reviews book_reviews_book_id_user_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.book_reviews
    ADD CONSTRAINT book_reviews_book_id_user_id_key UNIQUE (book_id, user_id);


--
-- Name: book_reviews book_reviews_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.book_reviews
    ADD CONSTRAINT book_reviews_pkey PRIMARY KEY (id);


--
-- Name: books books_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.books
    ADD CONSTRAINT books_pkey PRIMARY KEY (id);


--
-- Name: cart_items cart_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cart_items
    ADD CONSTRAINT cart_items_pkey PRIMARY KEY (id);


--
-- Name: carts carts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.carts
    ADD CONSTRAINT carts_pkey PRIMARY KEY (id);


--
-- Name: carts carts_user_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.carts
    ADD CONSTRAINT carts_user_id_key UNIQUE (user_id);


--
-- Name: daily_question_answers daily_question_answers_daily_question_id_user_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.daily_question_answers
    ADD CONSTRAINT daily_question_answers_daily_question_id_user_id_key UNIQUE (daily_question_id, user_id);


--
-- Name: daily_question_answers daily_question_answers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.daily_question_answers
    ADD CONSTRAINT daily_question_answers_pkey PRIMARY KEY (id);


--
-- Name: daily_questions daily_questions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.daily_questions
    ADD CONSTRAINT daily_questions_pkey PRIMARY KEY (id);


--
-- Name: discount_code_books discount_code_books_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.discount_code_books
    ADD CONSTRAINT discount_code_books_pkey PRIMARY KEY (discount_code_id, book_id);


--
-- Name: discount_code_usages discount_code_usages_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.discount_code_usages
    ADD CONSTRAINT discount_code_usages_pkey PRIMARY KEY (id);


--
-- Name: discount_codes discount_codes_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.discount_codes
    ADD CONSTRAINT discount_codes_code_key UNIQUE (code);


--
-- Name: discount_codes discount_codes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.discount_codes
    ADD CONSTRAINT discount_codes_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: publishers publishers_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.publishers
    ADD CONSTRAINT publishers_name_key UNIQUE (name);


--
-- Name: publishers publishers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.publishers
    ADD CONSTRAINT publishers_pkey PRIMARY KEY (id);


--
-- Name: purchases purchases_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.purchases
    ADD CONSTRAINT purchases_pkey PRIMARY KEY (id);


--
-- Name: purchases purchases_transaction_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.purchases
    ADD CONSTRAINT purchases_transaction_id_key UNIQUE (transaction_id);


--
-- Name: question_ai_conversations question_ai_conversations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.question_ai_conversations
    ADD CONSTRAINT question_ai_conversations_pkey PRIMARY KEY (id);


--
-- Name: question_ai_conversations question_ai_conversations_user_id_question_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.question_ai_conversations
    ADD CONSTRAINT question_ai_conversations_user_id_question_id_key UNIQUE (user_id, question_id);


--
-- Name: question_ai_messages question_ai_messages_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.question_ai_messages
    ADD CONSTRAINT question_ai_messages_pkey PRIMARY KEY (id);


--
-- Name: question_results question_results_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.question_results
    ADD CONSTRAINT question_results_pkey PRIMARY KEY (id);


--
-- Name: questions questions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.questions
    ADD CONSTRAINT questions_pkey PRIMARY KEY (id);


--
-- Name: test_results test_results_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.test_results
    ADD CONSTRAINT test_results_pkey PRIMARY KEY (id);


--
-- Name: tests tests_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tests
    ADD CONSTRAINT tests_pkey PRIMARY KEY (id);


--
-- Name: topics topics_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.topics
    ADD CONSTRAINT topics_pkey PRIMARY KEY (id);


--
-- Name: book_reviews uk1ew19gxgp7nujgcbfx9lsm7l4; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.book_reviews
    ADD CONSTRAINT uk1ew19gxgp7nujgcbfx9lsm7l4 UNIQUE (book_id, user_id);


--
-- Name: users uk6dotkott2kjsp8vw4d0m25fb7; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uk6dotkott2kjsp8vw4d0m25fb7 UNIQUE (email);


--
-- Name: publishers uk_publisher_name; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.publishers
    ADD CONSTRAINT uk_publisher_name UNIQUE (name);


--
-- Name: user_books uk_user_book; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_books
    ADD CONSTRAINT uk_user_book PRIMARY KEY (user_id, book_id);


--
-- Name: question_ai_conversations uk_user_question_conversation; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.question_ai_conversations
    ADD CONSTRAINT uk_user_question_conversation UNIQUE (user_id, question_id);


--
-- Name: users uk_users_email; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uk_users_email UNIQUE (email);


--
-- Name: users uk_users_phone_number; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uk_users_phone_number UNIQUE (phone_number);


--
-- Name: users uk_users_username; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uk_users_username UNIQUE (username);


--
-- Name: daily_question_answers ukh5ang59dw2vmgij61gl0gyuvk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.daily_question_answers
    ADD CONSTRAINT ukh5ang59dw2vmgij61gl0gyuvk UNIQUE (daily_question_id, user_id);


--
-- Name: users ukkwds03ohobcd8p6eowkw0f5bm; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT ukkwds03ohobcd8p6eowkw0f5bm UNIQUE (phone_number);


--
-- Name: users ukr43af9ap4edm43mmtq01oddj6; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT ukr43af9ap4edm43mmtq01oddj6 UNIQUE (username);


--
-- Name: user_flagged_questions user_flagged_questions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_flagged_questions
    ADD CONSTRAINT user_flagged_questions_pkey PRIMARY KEY (user_id, question_id);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: idx_book_exam_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_book_exam_type ON public.books USING btree (exam_type);


--
-- Name: idx_book_grade_level; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_book_grade_level ON public.books USING btree (grade_level);


--
-- Name: idx_book_paid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_book_paid ON public.books USING btree (paid);


--
-- Name: idx_book_price; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_book_price ON public.books USING btree (price);


--
-- Name: idx_book_review_book_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_book_review_book_id ON public.book_reviews USING btree (book_id);


--
-- Name: idx_book_review_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_book_review_user_id ON public.book_reviews USING btree (user_id);


--
-- Name: idx_discount_code_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_discount_code_active ON public.discount_codes USING btree (is_active);


--
-- Name: idx_discount_code_code; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_discount_code_code ON public.discount_codes USING btree (code);


--
-- Name: idx_discount_code_usage_code; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_discount_code_usage_code ON public.discount_code_usages USING btree (discount_code_id);


--
-- Name: idx_discount_code_usage_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_discount_code_usage_date ON public.discount_code_usages USING btree (used_at);


--
-- Name: idx_discount_code_usage_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_discount_code_usage_user ON public.discount_code_usages USING btree (user_id);


--
-- Name: idx_test_result_completed_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_test_result_completed_at ON public.test_results USING btree (completed_at);


--
-- Name: idx_test_result_test_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_test_result_test_id ON public.test_results USING btree (test_id);


--
-- Name: idx_test_result_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_test_result_user_id ON public.test_results USING btree (user_id);


--
-- Name: idx_user_email; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_email ON public.users USING btree (email);


--
-- Name: idx_user_username; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_username ON public.users USING btree (username);


--
-- Name: book_codes book_codes_book_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.book_codes
    ADD CONSTRAINT book_codes_book_id_fkey FOREIGN KEY (book_id) REFERENCES public.books(id) ON DELETE CASCADE;


--
-- Name: book_codes book_codes_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.book_codes
    ADD CONSTRAINT book_codes_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: book_reviews book_reviews_book_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.book_reviews
    ADD CONSTRAINT book_reviews_book_id_fkey FOREIGN KEY (book_id) REFERENCES public.books(id) ON DELETE CASCADE;


--
-- Name: book_reviews book_reviews_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.book_reviews
    ADD CONSTRAINT book_reviews_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: books books_publisher_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.books
    ADD CONSTRAINT books_publisher_id_fkey FOREIGN KEY (publisher_id) REFERENCES public.publishers(id);


--
-- Name: cart_items cart_items_book_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cart_items
    ADD CONSTRAINT cart_items_book_id_fkey FOREIGN KEY (book_id) REFERENCES public.books(id) ON DELETE CASCADE;


--
-- Name: cart_items cart_items_cart_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cart_items
    ADD CONSTRAINT cart_items_cart_id_fkey FOREIGN KEY (cart_id) REFERENCES public.carts(id) ON DELETE CASCADE;


--
-- Name: carts carts_discount_code_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.carts
    ADD CONSTRAINT carts_discount_code_id_fkey FOREIGN KEY (discount_code_id) REFERENCES public.discount_codes(id);


--
-- Name: carts carts_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.carts
    ADD CONSTRAINT carts_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: daily_question_answers daily_question_answers_daily_question_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.daily_question_answers
    ADD CONSTRAINT daily_question_answers_daily_question_id_fkey FOREIGN KEY (daily_question_id) REFERENCES public.daily_questions(id);


--
-- Name: daily_question_answers daily_question_answers_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.daily_question_answers
    ADD CONSTRAINT daily_question_answers_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: discount_code_books discount_code_books_book_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.discount_code_books
    ADD CONSTRAINT discount_code_books_book_id_fkey FOREIGN KEY (book_id) REFERENCES public.books(id) ON DELETE CASCADE;


--
-- Name: discount_code_books discount_code_books_discount_code_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.discount_code_books
    ADD CONSTRAINT discount_code_books_discount_code_id_fkey FOREIGN KEY (discount_code_id) REFERENCES public.discount_codes(id) ON DELETE CASCADE;


--
-- Name: discount_code_usages discount_code_usages_discount_code_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.discount_code_usages
    ADD CONSTRAINT discount_code_usages_discount_code_id_fkey FOREIGN KEY (discount_code_id) REFERENCES public.discount_codes(id);


--
-- Name: discount_code_usages discount_code_usages_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.discount_code_usages
    ADD CONSTRAINT discount_code_usages_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: discount_codes discount_codes_publisher_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.discount_codes
    ADD CONSTRAINT discount_codes_publisher_id_fkey FOREIGN KEY (publisher_id) REFERENCES public.publishers(id);


--
-- Name: purchases purchases_book_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.purchases
    ADD CONSTRAINT purchases_book_id_fkey FOREIGN KEY (book_id) REFERENCES public.books(id) ON DELETE CASCADE;


--
-- Name: purchases purchases_discount_code_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.purchases
    ADD CONSTRAINT purchases_discount_code_id_fkey FOREIGN KEY (discount_code_id) REFERENCES public.discount_codes(id);


--
-- Name: purchases purchases_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.purchases
    ADD CONSTRAINT purchases_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: question_ai_conversations question_ai_conversations_question_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.question_ai_conversations
    ADD CONSTRAINT question_ai_conversations_question_id_fkey FOREIGN KEY (question_id) REFERENCES public.questions(id) ON DELETE CASCADE;


--
-- Name: question_ai_conversations question_ai_conversations_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.question_ai_conversations
    ADD CONSTRAINT question_ai_conversations_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: question_ai_messages question_ai_messages_conversation_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.question_ai_messages
    ADD CONSTRAINT question_ai_messages_conversation_id_fkey FOREIGN KEY (conversation_id) REFERENCES public.question_ai_conversations(id) ON DELETE CASCADE;


--
-- Name: question_hints question_hints_question_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.question_hints
    ADD CONSTRAINT question_hints_question_id_fkey FOREIGN KEY (question_id) REFERENCES public.questions(id) ON DELETE CASCADE;


--
-- Name: question_results question_results_question_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.question_results
    ADD CONSTRAINT question_results_question_id_fkey FOREIGN KEY (question_id) REFERENCES public.questions(id) ON DELETE CASCADE;


--
-- Name: question_results question_results_test_result_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.question_results
    ADD CONSTRAINT question_results_test_result_id_fkey FOREIGN KEY (test_result_id) REFERENCES public.test_results(id) ON DELETE CASCADE;


--
-- Name: questions questions_test_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.questions
    ADD CONSTRAINT questions_test_id_fkey FOREIGN KEY (test_id) REFERENCES public.tests(id) ON DELETE CASCADE;


--
-- Name: test_results test_results_test_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.test_results
    ADD CONSTRAINT test_results_test_id_fkey FOREIGN KEY (test_id) REFERENCES public.tests(id) ON DELETE CASCADE;


--
-- Name: test_results test_results_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.test_results
    ADD CONSTRAINT test_results_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: tests tests_topic_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tests
    ADD CONSTRAINT tests_topic_id_fkey FOREIGN KEY (topic_id) REFERENCES public.topics(id) ON DELETE CASCADE;


--
-- Name: topics topics_book_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.topics
    ADD CONSTRAINT topics_book_id_fkey FOREIGN KEY (book_id) REFERENCES public.books(id) ON DELETE CASCADE;


--
-- Name: user_books user_books_book_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_books
    ADD CONSTRAINT user_books_book_id_fkey FOREIGN KEY (book_id) REFERENCES public.books(id) ON DELETE CASCADE;


--
-- Name: user_books user_books_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_books
    ADD CONSTRAINT user_books_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: user_flagged_questions user_flagged_questions_question_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_flagged_questions
    ADD CONSTRAINT user_flagged_questions_question_id_fkey FOREIGN KEY (question_id) REFERENCES public.questions(id) ON DELETE CASCADE;


--
-- Name: user_flagged_questions user_flagged_questions_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_flagged_questions
    ADD CONSTRAINT user_flagged_questions_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

\unrestrict MyqRY5HZek6ke287Fhgcsm1pl4dwXjPExbSxJVxdNIgoHu56Aj5hz0hCJGj7wuj

